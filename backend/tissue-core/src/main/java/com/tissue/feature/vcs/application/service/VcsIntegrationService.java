package com.tissue.feature.vcs.application.service;

import com.tissue.feature.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueTransitionService;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueBranch;
import com.tissue.feature.issue.domain.IssuePullRequest;
import com.tissue.feature.issue.domain.service.IssueBranchSyncService;
import com.tissue.feature.issue.domain.service.IssueBranchSyncService.BranchSync;
import com.tissue.feature.issue.domain.service.IssuePullRequestSyncService;
import com.tissue.feature.issue.domain.support.IssueKeyExtractor;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.application.dto.GitPushDto;
import com.tissue.feature.vcs.application.dto.VcsEventResult;
import com.tissue.feature.vcs.application.port.repository.ProjectVcsIntegrationRepository;
import com.tissue.feature.vcs.application.port.usecase.GitProviderUseCase;
import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
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

/**
 * Applies an inbound VCS event to the domain: links branches, records the PR connection, and runs the
 * workflow automation configured for the issue's workflow.
 *
 * <p>Every exit returns a {@link VcsEventResult} describing what happened. Most events legitimately do
 * nothing (a branch that names no issue, a PR against a workflow with no automation), and the inbox stores
 * that reason so an operator can tell "nothing to do" apart from "something broke".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VcsIntegrationService implements GitProviderUseCase {

    private final IssueTransitionService issueTransitionService;
    private final ProjectVcsIntegrationRepository integrationRepository;
    private final IssueQueryRepository issueQueryRepository;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final IssueEventPublisher eventPublisher;
    private final IssueBranchSyncService issueBranchSyncService;
    private final IssuePullRequestSyncService issuePullRequestSyncService;

    private static final String REFS_HEADS_PREFIX = "refs/heads/";

    @Override
    @Transactional
    public VcsEventResult handlePushEvent(GitPushDto gitPush) {
        log.info("VCS Push event received for project: {}. Ref: {}", gitPush.projectKey(), gitPush.ref());

        if (gitPush.ref() == null || !gitPush.ref().startsWith(REFS_HEADS_PREFIX)) {
            return VcsEventResult.skipped("Not a branch ref: " + gitPush.ref());
        }

        IntegrationLookup integration = resolveIntegration(gitPush.projectKey(), gitPush.provider());
        if (integration.integration() == null) {
            return VcsEventResult.skipped(integration.detail());
        }

        IssueLookup lookup = resolveIssue(gitPush.ref(), gitPush.projectKey());
        Issue issue = lookup.issue();
        if (issue == null) {
            return VcsEventResult.skipped(lookup.detail());
        }

        BranchSync sync = issueBranchSyncService.syncBranch(issue, gitPush);
        IssueBranch branch = sync.branch();
        ProjectMember actor = findProjectMemberOrNull(issue.getProjectKey(), gitPush.pusherEmail());

        // only the first push records activity: every later push to the same branch would repeat an
        // identical entry, burying the issue's real history under it. The branch itself still shows the
        // latest commit, so nothing is lost by staying quiet here.
        if (sync.newlyLinked()) {
            eventPublisher.publishBranchLinked(issue, branch, actor);
            return VcsEventResult.handled("Linked branch %s to %s".formatted(branch.getBranchName(), issue.getKey()));
        }

        return VcsEventResult.handled("Updated branch %s on %s".formatted(branch.getBranchName(), issue.getKey()));
    }

    @Override
    @Transactional
    public VcsEventResult handlePullRequest(GitPrDto gitPr) {
        log.info(
                "VCS Pull Request event received for project: {}. Action: {}, Title: {}",
                gitPr.projectKey(),
                gitPr.action(),
                gitPr.title());

        IntegrationLookup integration = resolveIntegration(gitPr.projectKey(), gitPr.provider());
        if (integration.integration() == null) {
            return VcsEventResult.skipped(integration.detail());
        }

        IssueLookup lookup = resolveIssue(gitPr.title(), gitPr.projectKey());
        Issue issue = lookup.issue();
        if (issue == null) {
            return VcsEventResult.skipped(lookup.detail());
        }

        // TODO: resolve the actor from the event's sender, not from the pull request's author.
        //  gitPr.authorEmail() is `pull_request.user` - who opened the pull request - so on a merge or a
        //  close this credits the author with an action someone else took. That actor flows into the
        //  activity entry below and into processWorkflowTransition, which records the automatic transition
        //  as performed by them. See the TODO in GithubPrPayload.toVcsDto for the payload-side change.
        ProjectMember actor = findProjectMemberOrNull(issue.getProjectKey(), gitPr.authorEmail());

        IssuePullRequest pullRequest = issuePullRequestSyncService.syncPullRequest(issue, gitPr);

        // GitHub sends a pull_request event for far more than opening and closing - a label change, a new
        // commit pushed to the PR. Those move nothing on the issue, and recording each one would drown the
        // activity feed; the pull request section carries their effect already.
        if (gitPr.action() != PrAction.UNKNOWN) {
            eventPublisher.publishVcsConnectionEvent(issue, gitPr, actor);
        }
        String transitionDetail = processWorkflowTransition(issue, gitPr, actor);

        String linked = pullRequest == null
                ? "Linked PR (%s) to %s".formatted(gitPr.action(), issue.getKey())
                : "Linked PR #%d (%s) to %s".formatted(pullRequest.getNumber(), pullRequest.getState(), issue.getKey());

        return VcsEventResult.handled("%s. %s".formatted(linked, transitionDetail));
    }

    private IntegrationLookup resolveIntegration(String projectKey, VcsProvider provider) {
        ProjectVcsIntegration integration = integrationRepository
                .findByProjectKeyAndProvider(projectKey, provider)
                .orElse(null);

        if (integration == null) {
            log.warn("VCS integration not found for project: {} and provider: {}", projectKey, provider);
            return new IntegrationLookup(null, "No %s integration for project %s".formatted(provider, projectKey));
        }

        if (integration.isInactive()) {
            log.info("VCS integration is inactive for project: {}. Skipping event processing.", projectKey);
            return new IntegrationLookup(
                    null, "%s integration is disabled for project %s".formatted(provider, projectKey));
        }

        return new IntegrationLookup(integration, "");
    }

    /**
     * Resolves the issue a webhook refers to, scoped to the project the webhook was delivered for. An issue
     * key is just text an author controls, so without the project check a caller holding one project's
     * webhook secret could drive issues in any other project.
     */
    private IssueLookup resolveIssue(@Nullable String text, String webhookProjectKey) {
        String issueKey = IssueKeyExtractor.extract(text);
        if (issueKey == null) {
            log.debug("No issue key found in text: {}", text);
            return new IssueLookup(null, "No issue key found in: " + text);
        }

        // TODO: Join fetch with IssueType and Workflow
        //  Consider wrapping it with IssueFinder
        Issue issue = issueQueryRepository.findByKey(issueKey).orElse(null);
        if (issue == null) {
            log.warn("Issue not found for key: {}", issueKey);
            return new IssueLookup(null, "No such issue: " + issueKey);
        }

        if (!Objects.equals(issue.getProjectKey(), webhookProjectKey)) {
            log.warn(
                    "Issue {} belongs to project {}, but the webhook was delivered for project {}. "
                            + "Ignoring cross-project reference.",
                    issueKey,
                    issue.getProjectKey(),
                    webhookProjectKey);
            return new IssueLookup(
                    null,
                    "Issue %s belongs to project %s, not %s"
                            .formatted(issueKey, issue.getProjectKey(), webhookProjectKey));
        }

        return new IssueLookup(issue, "");
    }

    @Nullable
    private ProjectMember findProjectMemberOrNull(String projectKey, @Nullable String email) {
        if (email == null) {
            return null;
        }

        return projectMemberQueryRepository
                .findWithMemberByEmailAndProjectKey(email, projectKey)
                .orElse(null);
    }

    private String processWorkflowTransition(Issue issue, GitPrDto gitPr, @Nullable ProjectMember matchedMember) {
        WorkflowTransition transition = resolveTransition(issue, gitPr.action());
        if (transition == null) {
            return "No transition configured for PR %s".formatted(gitPr.action());
        }

        if (currentStateNotMatchTransitionSourceState(issue, transition)) {
            log.info(
                    "Issue {} is currently in state '{}', "
                            + "but the VCS automation transition requires the state to be '{}'. "
                            + "Skipping automatic transition.",
                    issue.getKey(),
                    issue.getCurrentState().getName().getDisplayName(),
                    transition.getSourceState().getName().getDisplayName());
            return "Transition skipped: issue is in '%s' but the automation requires '%s'"
                    .formatted(
                            issue.getCurrentState().getName().getDisplayName(),
                            transition.getSourceState().getName().getDisplayName());
        }

        if (matchedMember == null) {
            performTransitionBySystem(issue, transition, gitPr);
            return "Transitioned to '%s' by system"
                    .formatted(transition.getTargetState().getName().getDisplayName());
        }

        performTransitionByMember(issue, transition, matchedMember);
        return "Transitioned to '%s' by %s"
                .formatted(transition.getTargetState().getName().getDisplayName(), matchedMember.getDisplayName());
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
                "Transitioning issue {} from '{}' to '{}' based on VCS event by matched member: {}",
                issue.getKey(),
                transition.getSourceState().getName().getDisplayName(),
                transition.getTargetState().getName().getDisplayName(),
                member.getDisplayName());

        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issue.getKey());

        issueTransitionService.performTransition(iid, transition.getId(), member.getMemberId());
    }

    private void performTransitionBySystem(Issue issue, WorkflowTransition transition, GitPrDto gitPr) {
        log.info(
                "Transitioning issue {} from '{}' to '{}' via System Automation "
                        + "(No matched member found for VCS author: {})",
                issue.getKey(),
                transition.getSourceState().getName().getDisplayName(),
                transition.getTargetState().getName().getDisplayName(),
                gitPr.authorEmail());

        String triggerReason =
                "%s PR %s".formatted(gitPr.provider().name(), gitPr.action().name());

        var cmd = PerformSystemTransitionCommand.builder()
                .vcsProvider(gitPr.provider())
                .vcsUserEmail(gitPr.authorEmail())
                .vcsUserName(gitPr.authorUsername())
                .triggerReason(triggerReason)
                .build();

        issueTransitionService.performTransitionBySystem(issue.getKey(), transition.getId(), cmd);
    }

    private boolean currentStateNotMatchTransitionSourceState(Issue issue, WorkflowTransition transition) {
        return !Objects.equals(issue.getCurrentState(), transition.getSourceState());
    }

    private record IntegrationLookup(@Nullable ProjectVcsIntegration integration, String detail) {}

    private record IssueLookup(@Nullable Issue issue, String detail) {}
}
