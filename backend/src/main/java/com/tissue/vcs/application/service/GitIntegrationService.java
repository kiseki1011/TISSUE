package com.tissue.vcs.application.service;

import com.tissue.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.application.service.IssueTransitionService;
import com.tissue.issue.domain.Issue;
import com.tissue.vcs.application.port.in.GitProviderUseCase;
import com.tissue.vcs.application.port.out.WorkspaceVcsIntegrationRepository;
import com.tissue.vcs.domain.GitPrDto;
import com.tissue.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.vcs.domain.enums.PrAction;
import com.tissue.vcs.domain.exception.WorkspaceVcsIntegrationNotFoundException;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.WorkspaceMember;
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
    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    // TODO: 개선필요할까? 엣지 케이스를 확실하게 커버하는지 알고 싶음
    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("\\b[A-Z][A-Z0-9]+-\\d+\\b");

    @Override
    @Transactional
    public void handlePullRequest(GitPrDto gitPr) {
        log.info("[VCS_PULL_REQUEST] action={}, workspace={}, title={}", gitPr.action(), gitPr.workspaceKey(), gitPr.title());

        WorkspaceVcsIntegration integration = integrationRepository
                .findByWorkspaceKey(gitPr.workspaceKey())
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(gitPr.workspaceKey()));

        // TODO: 이 로직은 왜? 추후에 gitlab도 제공한다면 어떻게?
        if (!integration.isGithubSyncEnabled()) {
            log.info("[VCS_PULL_REQUEST] GitHub sync is disabled for workspace={}", gitPr.workspaceKey());
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

        findActor(gitPr.authorEmail(), gitPr.workspaceKey())
                .ifPresent(actor -> log.info(
                        "[VCS_PULL_REQUEST] Identified matching author username={}, email={}",
                        actor.getMember().getUsername(),
                        actor.getMember().getEmail()));

        // TODO: Add Activity Log - 이벤트 발행해서 ActivityLogEventListener에서 처리

        processWorkflowTransition(issue, gitPr);
    }

    @Nullable
    private String extractIssueKey(@Nullable String title) {
        if (title == null) {
            return null;
        }

        Matcher matcher = ISSUE_KEY_PATTERN.matcher(title);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private Optional<WorkspaceMember> findActor(@Nullable String email, String workspaceKey) {
        if (email == null) {
            return Optional.empty();
        }

        return workspaceMemberQueryRepository.findByMember_EmailAndWorkspaceKey(email, workspaceKey);
    }

    private void processWorkflowTransition(Issue issue, GitPrDto gitPr) {
        WorkflowTransition transition = null;
        switch (gitPr.action()) {
            case PrAction.OPENED, PrAction.REOPENED ->
                transition = issue.getIssueType().getWorkflow().getVcsSettings().getVcsPrOpenedTransition();
            case MERGED ->
                transition = issue.getIssueType().getWorkflow().getVcsSettings().getVcsPrMergedTransition();
            default -> log.debug("PR ignored");
        }

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

        log.info(
                "[VCS_PULL_REQUEST] Transitioning issue {}:{} via VCS automation: {} -> {}",
                issue.getWorkspaceKey(),
                issue.getKey(),
                transition.getSourceState().getName().getDisplay(),
                transition.getTargetState().getName().getDisplay());

        var cmd = PerformSystemTransitionCommand.builder()
                .workspaceKey(issue.getWorkspaceKey())
                .projectKey(issue.getProjectKey())
                .issueKey(issue.getKey())
                .transitionId(transition.getId())
                .vcsUserEmail(gitPr.authorEmail())
                .vcsUserName(gitPr.authorUsername())
                .build();

        issueTransitionService.performTransitionBySystem(cmd);
    }

    private boolean currentStateNotMatchTransitionSourceState(Issue issue, WorkflowTransition transition) {
        return !Objects.equals(issue.getCurrentState(), transition.getSourceState());
    }
}
