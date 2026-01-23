package com.tissue.vcs.application.service;

import com.tissue.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.application.service.IssueTransitionService;
import com.tissue.issue.application.service.event.IssueEventPublisher;
import com.tissue.issue.domain.Issue;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.domain.ProjectMember;
import com.tissue.vcs.application.port.in.GitProviderUseCase;
import com.tissue.vcs.application.port.out.WorkspaceVcsIntegrationRepository;
import com.tissue.vcs.domain.GitPrDto;
import com.tissue.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.vcs.domain.enums.PrAction;
import com.tissue.vcs.domain.exception.WorkspaceVcsIntegrationNotFoundException;
import com.tissue.workflow.domain.WorkflowTransition;
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
public class GitIntegrationService implements GitProviderUseCase {

    private final IssueTransitionService issueTransitionService;
    private final WorkspaceVcsIntegrationRepository integrationRepository;
    private final IssueQueryRepository issueQueryRepository;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final IssueEventPublisher eventPublisher;

    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("\\b[A-Za-z][A-Za-z0-9]+-\\d+\\b");

    @Override
    @Transactional
    public void handlePullRequest(GitPrDto gitPr) {
        log.info("[VCS_PULL_REQUEST] action={}, workspace={}, title={}", gitPr.action(), gitPr.workspaceKey(), gitPr.title());

        WorkspaceVcsIntegration integration = integrationRepository
                .findByWorkspaceKey(gitPr.workspaceKey())
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(gitPr.workspaceKey()));

        if (!integration.isSyncEnabled()) {
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

        eventPublisher.publishVcsConnectionEvent(issue, gitPr);

        processWorkflowTransition(issue, gitPr);
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

    private void processWorkflowTransition(Issue issue, GitPrDto gitPr) {
        WorkflowTransition transition = resolveTransition(issue, gitPr.action());

        if (transition == null) {
            return;
        }

        if (currentStateNotMatchTransitionSourceState(issue, transition)) {
            log.info(
                    "[VCS_PULL_REQUEST] Issue {}:{} is in state {}, but VCS transition requires state {}. Skipping automation.",
                    issue.getWorkspaceKey(),
                    issue.getKey(),
                    issue.getCurrentState().getName().getDisplay(),
                    transition.getSourceState().getName().getDisplay());
            return;
        }

        Optional<ProjectMember> matchedMember = findProjectMember(gitPr, issue.getProjectKey());

        if (matchedMember.isPresent()) {
            performTransitionWithMember(issue, transition, matchedMember.get());
        } else {
            performTransitionBySystem(issue, transition, gitPr);
        }
    }

    private WorkflowTransition resolveTransition(Issue issue, PrAction action) {
        return switch (action) {
            case OPENED, REOPENED -> issue.getIssueType().getWorkflow().getVcsSettings().getVcsPrOpenedTransition();
            case MERGED -> issue.getIssueType().getWorkflow().getVcsSettings().getVcsPrMergedTransition();
            default -> null;
        };
    }

    private Optional<ProjectMember> findProjectMember(GitPrDto gitPr, String projectKey) {
        if (gitPr.authorEmail() == null) {
            return Optional.empty();
        }
        return projectMemberQueryRepository.findWithWorkspaceMemberByEmailAndKeys(gitPr.authorEmail(), projectKey, gitPr.workspaceKey());
    }

    private void performTransitionWithMember(Issue issue, WorkflowTransition transition, ProjectMember member) {
        log.info(
                "[VCS_PULL_REQUEST] Transitioning issue {}:{} by matched member {}",
                issue.getWorkspaceKey(),
                issue.getKey(),
                member.getWorkspaceMember().getDisplayName());

        ProjectMemberContext context = ProjectMemberContext.from(member);

        issueTransitionService.performTransition(new PerformTransitionCommand(
                issue.getKey(),
                transition.getId(),
                context
        ));
    }

    private void performTransitionBySystem(Issue issue, WorkflowTransition transition, GitPrDto gitPr) {
        log.info(
                "[VCS_PULL_REQUEST] Transitioning issue {}:{} via System Automation (No matched member)",
                issue.getWorkspaceKey(),
                issue.getKey());

        String triggerReason = "GitHub PR #%s %s".formatted(
                gitPr.htmlUrl() != null ? "Link" : "",
                gitPr.action().name()
        );

        var cmd = PerformSystemTransitionCommand.builder()
                .workspaceKey(issue.getWorkspaceKey())
                .projectKey(issue.getProjectKey())
                .issueKey(issue.getKey())
                .transitionId(transition.getId())
                .vcsUserEmail(gitPr.authorEmail())
                .vcsUserName(gitPr.authorUsername())
                .triggerReason(triggerReason)
                .build();

        issueTransitionService.performTransitionBySystem(cmd);
    }

    private boolean currentStateNotMatchTransitionSourceState(Issue issue, WorkflowTransition transition) {
        return !Objects.equals(issue.getCurrentState(), transition.getSourceState());
    }
}
