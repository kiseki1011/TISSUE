package com.tissue.feature.vcs.application.service;

import com.tissue.feature.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueTransitionService;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueBranch;
import com.tissue.feature.issue.domain.service.IssueBranchSyncService;
import com.tissue.feature.issue.domain.support.IssueKeyExtractor;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.application.dto.GitPushDto;
import com.tissue.feature.vcs.application.port.repository.WorkspaceVcsIntegrationRepository;
import com.tissue.feature.vcs.application.port.usecase.GitProviderUseCase;
import com.tissue.feature.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.Objects;
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
    private final IssueBranchSyncService issueBranchSyncService;

    private static final String REFS_HEADS_PREFIX = "refs/heads/";

    @Override
    @Transactional
    public void handlePushEvent(GitPushDto gitPush) {
        log.info("VCS Push event received for workspace: {}. Ref: {}", gitPush.workspaceKey(), gitPush.ref());

        if (gitPush.ref() == null || !gitPush.ref().startsWith(REFS_HEADS_PREFIX)) {
            log.debug("Ignored non-branch push ref: {}", gitPush.ref());
            return;
        }

        var integration = getActiveIntegrationOrNull(gitPush.workspaceKey(), gitPush.provider());
        if (integration == null) {
            return;
        }

        var issue = resolveIssueOrNull(gitPush.workspaceKey(), gitPush.ref());
        if (issue == null) {
            return;
        }

        IssueBranch branch = issueBranchSyncService.syncBranch(issue, gitPush);
        var actor = findProjectMemberOrNull(gitPush.workspaceKey(), issue.getProjectKey(), gitPush.pusherEmail());

        eventPublisher.publishBranchLinked(issue, branch, actor);
    }

    @Override
    @Transactional
    public void handlePullRequest(GitPrDto gitPr) {
        log.info(
                "VCS Pull Request event received for workspace: {}. Action: {}, Title: {}",
                gitPr.workspaceKey(),
                gitPr.action(),
                gitPr.title());

        var integration = getActiveIntegrationOrNull(gitPr.workspaceKey(), gitPr.provider());
        if (integration == null) {
            return;
        }

        var issue = resolveIssueOrNull(gitPr.workspaceKey(), gitPr.title());
        if (issue == null) {
            return;
        }

        var actor = findProjectMemberOrNull(gitPr.workspaceKey(), issue.getProjectKey(), gitPr.authorEmail());

        eventPublisher.publishVcsConnectionEvent(issue, gitPr, actor);
        processWorkflowTransition(issue, gitPr, actor);
    }

    @Nullable
    private WorkspaceVcsIntegration getActiveIntegrationOrNull(String workspaceKey, VcsProvider provider) {
        var integration = integrationRepository
                .findByWorkspaceKeyAndProvider(workspaceKey, provider)
                .orElse(null);

        if (integration == null) {
            log.warn("VCS integration not found for workspace: {} and provider: {}", workspaceKey, provider);
            return null;
        }

        if (integration.isInactive()) {
            log.info("VCS integration is inactive for workspace: {}. Skipping event processing.", workspaceKey);
            return null;
        }

        return integration;
    }

    @Nullable
    private Issue resolveIssueOrNull(String workspaceKey, @Nullable String text) {
        String issueKey = IssueKeyExtractor.extract(text);
        if (issueKey == null) {
            log.debug("No issue key found in text: {}", text);
            return null;
        }

        // TODO: Join fetch with IssueType and Workflow
        //  Consider wrapping it with IssueFinder
        return issueQueryRepository
                .findByKeyAndWorkspaceKey(issueKey, workspaceKey)
                .orElseGet(() -> {
                    log.warn("Issue not found for key: {} in workspace: {}", issueKey, workspaceKey);
                    return null;
                });
    }

    @Nullable
    private ProjectMember findProjectMemberOrNull(String workspaceKey, String projectKey, @Nullable String email) {
        if (email == null) {
            return null;
        }

        return projectMemberQueryRepository
                .findWithWorkspaceMemberByEmailAndKeys(email, projectKey, workspaceKey)
                .orElse(null);
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

        if (matchedMember == null) {
            performTransitionBySystem(issue, transition, gitPr);
            return;
        }
        performTransitionByMember(issue, transition, matchedMember);
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

    private void performTransitionByMember(Issue issue, WorkflowTransition transition, ProjectMember member) {
        log.info(
                "Transitioning issue {}:{} from '{}' to '{}' based on VCS event by matched member: {}",
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
