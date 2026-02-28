package com.tissue.feature.vcs.application.service;

import com.tissue.feature.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueTransitionService;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueBranch;
import com.tissue.feature.issue.domain.support.IssueKeyExtractor;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.application.dto.GitPushDto;
import com.tissue.feature.vcs.application.port.repository.WorkspaceVcsIntegrationRepository;
import com.tissue.feature.vcs.application.port.usecase.GitProviderUseCase;
import com.tissue.feature.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.feature.vcs.domain.exception.WorkspaceVcsIntegrationNotFoundException;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VcsIntegrationService implements GitProviderUseCase {

    private final IssueTransitionService issueTransitionService;
    private final WorkspaceVcsIntegrationRepository integrationRepository;
    private final IssueQueryRepository issueQueryRepository;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final IssueEventPublisher eventPublisher;

    private static final String REFS_HEADS_PREFIX = "refs/heads/";
    private static final String GITHUB_TREE_PATH = "/tree/";

    @Override
    @Transactional
    public void handlePullRequest(GitPrDto gitPr) {
        log.info(
                "VCS Pull Request event received for workspace: {}. Action: {}, Title: {}",
                gitPr.workspaceKey(),
                gitPr.action(),
                gitPr.title());

        WorkspaceVcsIntegration integration = integrationRepository
                .findByWorkspaceKeyAndProvider(gitPr.workspaceKey(), gitPr.provider())
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(
                        gitPr.workspaceKey(), gitPr.provider().toString()));

        if (!integration.isActive()) {
            log.info("VCS Integration is inactive for workspace: {}. Skipping PR processing.", gitPr.workspaceKey());
            return;
        }

        String issueKey = IssueKeyExtractor.extract(gitPr.title());
        if (issueKey == null) {
            log.debug("No issue key found in PR title: {}", gitPr.title());
            return;
        }

        Issue issue = issueQueryRepository
                .findByKeyAndWorkspaceKey(issueKey, gitPr.workspaceKey())
                .orElseGet(() -> {
                    log.warn("Issue not found for key: {} in workspace: {}", issueKey, gitPr.workspaceKey());
                    return null;
                });

        if (issue == null) {
            return;
        }

        Optional<ProjectMember> matchedMember = findProjectMember(gitPr, issue.getProjectKey());
        eventPublisher.publishVcsConnectionEvent(issue, gitPr, matchedMember.orElse(null));

        processWorkflowTransition(issue, gitPr, matchedMember.orElse(null));
    }

    @Override
    @Transactional
    public void handlePushEvent(GitPushDto gitPush) {
        log.info("VCS Push event received for workspace: {}. Ref: {}", gitPush.workspaceKey(), gitPush.ref());

        if (gitPush.ref() == null || !gitPush.ref().startsWith(REFS_HEADS_PREFIX)) {
            log.debug("Ignored non-branch push ref: {}", gitPush.ref());
            return;
        }

        if (gitPush.repoUrl() == null) {
            log.warn("Ignored push event without repository URL for workspace: {}", gitPush.workspaceKey());
            return;
        }

        WorkspaceVcsIntegration integration = integrationRepository
                .findByWorkspaceKeyAndProvider(gitPush.workspaceKey(), gitPush.provider())
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(
                        gitPush.workspaceKey(), gitPush.provider().toString()));

        if (!integration.isActive()) {
            log.info(
                    "VCS Integration is inactive for workspace: {}. Skipping push processing.", gitPush.workspaceKey());
            return;
        }

        String issueKey = IssueKeyExtractor.extract(gitPush.ref());
        if (issueKey == null) {
            log.debug("No issue key found in push ref: {}", gitPush.ref());
            return;
        }

        Issue issue = issueQueryRepository
                .findByKeyAndWorkspaceKey(issueKey, gitPush.workspaceKey())
                .orElse(null);

        if (issue == null) {
            log.warn("Issue not found for key: {} in workspace: {}", issueKey, gitPush.workspaceKey());
            return;
        }

        String branchName = gitPush.ref().replace(REFS_HEADS_PREFIX, "");
        String branchUrl = gitPush.repoUrl() + GITHUB_TREE_PATH + branchName;

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

        ProjectMember actor = null;
        if (gitPush.pusherEmail() != null) {
            actor = projectMemberQueryRepository
                    .findWithWorkspaceMemberByEmailAndKeys(
                            gitPush.pusherEmail(), issue.getProjectKey(), issue.getWorkspaceKey())
                    .orElse(null);
        }

        eventPublisher.publishBranchLinked(issue, branch, actor);
    }

    private void processWorkflowTransition(Issue issue, GitPrDto gitPr, @Nullable ProjectMember matchedMember) {
        WorkflowTransition transition = resolveTransition(issue, gitPr.action());

        if (transition == null) {
            return;
        }

        if (currentStateNotMatchTransitionSourceState(issue, transition)) {
            log.info(
                    "Issue {}:{} is currently in state '{}', "
                            + "but the VCS automation transition requires the state to be '{}'. "
                            + "Skipping automatic transition.",
                    issue.getWorkspaceKey(),
                    issue.getKey(),
                    issue.getCurrentState().getName().getDisplay(),
                    transition.getSourceState().getName().getDisplay());
            return;
        }

        if (matchedMember != null) {
            performTransitionWithMember(issue, transition, matchedMember);
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
                "Transitioning issue {}:{} from '{}' to '{}' based on VCS activity by matched member: {}",
                issue.getWorkspaceKey(),
                issue.getKey(),
                transition.getSourceState().getName().getDisplay(),
                transition.getTargetState().getName().getDisplay(),
                member.getWorkspaceMember().getDisplayName());

        IssueIdentifier issueIdentifier =
                IssueIdentifier.of(issue.getWorkspaceKey(), issue.getProjectKey(), issue.getKey());

        issueTransitionService.performTransition(issueIdentifier, transition.getId(), member.getMemberId());
    }

    private void performTransitionBySystem(Issue issue, WorkflowTransition transition, GitPrDto gitPr) {
        log.info(
                "Transitioning issue {}:{} from '{}' to '{}' via System Automation "
                        + "(No matched member found for VCS author: {})",
                issue.getWorkspaceKey(),
                issue.getKey(),
                transition.getSourceState().getName().getDisplay(),
                transition.getTargetState().getName().getDisplay(),
                gitPr.authorEmail());

        String triggerReason = "%s PR #%s %s"
                .formatted(
                        gitPr.provider().name(),
                        gitPr.htmlUrl() != null ? "Link" : "",
                        gitPr.action().name());

        var cmd = PerformSystemTransitionCommand.builder()
                .vcsProvider(gitPr.provider())
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
