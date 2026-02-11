package com.tissue.feature.vcs.application.service;

import com.tissue.feature.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueTransitionService;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueBranch;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.application.dto.GitPushDto;
import com.tissue.feature.vcs.application.port.repository.WorkspaceVcsIntegrationRepository;
import com.tissue.feature.vcs.application.port.usecase.GitProviderUseCase;
import com.tissue.feature.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.exception.WorkspaceVcsIntegrationNotFoundException;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubIntegrationService implements GitProviderUseCase {

    private final IssueTransitionService issueTransitionService;
    private final WorkspaceVcsIntegrationRepository integrationRepository;
    private final IssueQueryRepository issueQueryRepository;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final IssueEventPublisher eventPublisher;

    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("\\b[A-Za-z][A-Za-z0-9]+-\\d+\\b");

    @Override
    @Transactional
    public void handlePullRequest(GitPrDto gitPr) {
        log.info(
                "[VCS_PULL_REQUEST] action={}, workspace={}, title={}",
                gitPr.action(),
                gitPr.workspaceKey(),
                gitPr.title());

        WorkspaceVcsIntegration integration = integrationRepository
                .findByWorkspaceKeyAndProvider(gitPr.workspaceKey(), VcsProvider.GITHUB)
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(gitPr.workspaceKey()));

        if (!integration.isActive()) {
            log.info("[VCS_PULL_REQUEST] Integration is inactive for workspace={}", gitPr.workspaceKey());
            return;
        }

        String issueKey = extractIssueKey(gitPr.title());
        if (issueKey == null) {
            log.debug("No issue key found in PR title");
            return;
        }

        Issue issue = issueQueryRepository
                .findByKeyAndWorkspaceKey(issueKey, gitPr.workspaceKey())
                .orElseGet(() -> {
                    log.warn("[VCS_PULL_REQUEST] Issue not found: {}:{}", gitPr.workspaceKey(), issueKey);
                    return null;
                });

        if (issue == null) {
            return;
        }

        Optional<ProjectMember> matchedMember = findProjectMember(gitPr, issue.getProjectKey());
        Long actorMemberId = matchedMember.map(ProjectMember::getMemberId).orElse(null);
        String actorDisplayName = matchedMember
                .map(pm -> pm.getWorkspaceMember().getDisplayName())
                .orElse(null);

        eventPublisher.publishVcsConnectionEvent(issue, gitPr, actorMemberId, actorDisplayName);

        processWorkflowTransition(issue, gitPr, matchedMember);
    }

    @Override
    @Transactional
    public void handlePushEvent(GitPushDto gitPush) {
        log.info("[VCS_PUSH] workspace={}, ref={}", gitPush.workspaceKey(), gitPush.ref());

        if (gitPush.ref() == null || !gitPush.ref().startsWith("refs/heads/")) {
            log.debug("[VCS_PUSH] Ignored non-branch push: {}", gitPush.ref());
            return;
        }

        if (gitPush.repoUrl() == null) {
            log.debug("[VCS_PUSH] Ignored push without repository URL");
            return;
        }

        WorkspaceVcsIntegration integration = integrationRepository
                .findByWorkspaceKeyAndProvider(gitPush.workspaceKey(), VcsProvider.GITHUB)
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(gitPush.workspaceKey()));

        if (!integration.isActive()) {
            log.info("[VCS_PUSH] Integration is inactive for workspace={}", gitPush.workspaceKey());
            return;
        }

        String issueKey = extractIssueKey(gitPush.ref());
        if (issueKey == null) {
            log.debug("[VCS_PUSH] No issue key found in ref: {}", gitPush.ref());
            return;
        }

        Issue issue = issueQueryRepository
                .findByKeyAndWorkspaceKey(issueKey, gitPush.workspaceKey())
                .orElse(null);

        if (issue == null) {
            log.warn("[VCS_PUSH] Issue not found: {}:{}", gitPush.workspaceKey(), issueKey);
            return;
        }

        String branchName = gitPush.ref().replace("refs/heads/", "");
        String branchUrl = gitPush.repoUrl() + "/tree/" + branchName;

        IssueBranch branch = issue.getBranches().stream()
                .filter(b -> b.getBranchName().equals(branchName))
                .findFirst()
                .orElse(null);

        if (branch == null) {
            branch = IssueBranch.create(
                    issue,
                    gitPush.repoUrl(),
                    branchName,
                    branchUrl,
                    gitPush.latestCommitHash(),
                    gitPush.latestCommitMessage(),
                    gitPush.latestCommitUrl(),
                    gitPush.pusherName(),
                    gitPush.occurredAt());
            issue.addBranch(branch);
        } else {
            branch.updateLatestCommit(
                    gitPush.latestCommitHash(),
                    gitPush.latestCommitMessage(),
                    gitPush.latestCommitUrl(),
                    gitPush.pusherName(),
                    gitPush.occurredAt());
        }

        Long actorMemberId = null;
        String actorDisplayName = null;
        if (gitPush.pusherEmail() != null) {
            Optional<ProjectMember> member = projectMemberQueryRepository.findWithWorkspaceMemberByEmailAndKeys(
                    gitPush.pusherEmail(), issue.getProjectKey(), issue.getWorkspaceKey());
            if (member.isPresent()) {
                actorMemberId = member.get().getMemberId();
                actorDisplayName = member.get().getWorkspaceMember().getDisplayName();
            }
        }

        eventPublisher.publishBranchLinked(issue, branch, actorMemberId, actorDisplayName);
    }

    @Nullable
    private String extractIssueKey(@Nullable String title) {
        if (title == null) {
            return null;
        }

        Matcher matcher = ISSUE_KEY_PATTERN.matcher(title);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return null;
    }

    // TODO: Needs refactoring. I dont want to use Optoinal as a parameter
    private void processWorkflowTransition(Issue issue, GitPrDto gitPr, Optional<ProjectMember> matchedMember) {
        WorkflowTransition transition = resolveTransition(issue, gitPr.action());

        if (transition == null) {
            return;
        }

        if (currentStateNotMatchTransitionSourceState(issue, transition)) {
            log.info(
                    "[VCS_PULL_REQUEST] Issue {}:{} is in state {}, but VCS transition requires state {}."
                            + " Skipping automation.",
                    issue.getWorkspaceKey(),
                    issue.getKey(),
                    issue.getCurrentState().getName().getDisplay(),
                    transition.getSourceState().getName().getDisplay());
            return;
        }

        if (matchedMember.isPresent()) {
            performTransitionWithMember(issue, transition, matchedMember.get());
        } else {
            performTransitionBySystem(issue, transition, gitPr);
        }
    }

    @Nullable
    private WorkflowTransition resolveTransition(Issue issue, PrAction action) {
        return switch (action) {
            case OPENED, REOPENED ->
                issue.getIssueType().getWorkflow().getVcsSettings().getVcsPrOpenedTransition();
            case MERGED -> issue.getIssueType().getWorkflow().getVcsSettings().getVcsPrMergedTransition();
            default -> null;
        };
    }

    private Optional<ProjectMember> findProjectMember(GitPrDto gitPr, String projectKey) {
        if (gitPr.authorEmail() == null) {
            return Optional.empty();
        }
        return projectMemberQueryRepository.findWithWorkspaceMemberByEmailAndKeys(
                gitPr.authorEmail(), projectKey, gitPr.workspaceKey());
    }

    private void performTransitionWithMember(Issue issue, WorkflowTransition transition, ProjectMember member) {
        log.info(
                "[VCS_PULL_REQUEST] Transitioning issue {}:{} by matched member {}",
                issue.getWorkspaceKey(),
                issue.getKey(),
                member.getWorkspaceMember().getDisplayName());

        ProjectMemberContext context = ProjectMemberContext.from(member);

        issueTransitionService.performTransition(issue.getKey(), transition.getId(), context);
    }

    private void performTransitionBySystem(Issue issue, WorkflowTransition transition, GitPrDto gitPr) {
        log.info(
                "[VCS_PULL_REQUEST] Transitioning issue {}:{} via System Automation (No matched member)",
                issue.getWorkspaceKey(),
                issue.getKey());

        String triggerReason = "GitHub PR #%s %s"
                .formatted(gitPr.htmlUrl() != null ? "Link" : "", gitPr.action().name());

        var cmd = PerformSystemTransitionCommand.builder()
                .vcsProvider(VcsProvider.GITHUB)
                .vcsUserEmail(gitPr.authorEmail())
                .vcsUserName(gitPr.authorUsername())
                .triggerReason(triggerReason)
                .build();

        issueTransitionService.performTransitionBySystem(
                issue.getKey(), transition.getId(), issue.getWorkspaceKey(), issue.getProjectKey(), cmd);
    }

    private boolean currentStateNotMatchTransitionSourceState(Issue issue, WorkflowTransition transition) {
        return !Objects.equals(issue.getCurrentState(), transition.getSourceState());
    }
}
