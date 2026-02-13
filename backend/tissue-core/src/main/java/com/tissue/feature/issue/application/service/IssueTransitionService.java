package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.feature.issue.application.port.usecase.IssueTransitionUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.application.service.validator.IssueValidator;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.domain.TransitionGuardConfig;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.feature.workflow.domain.guard.TransitionGuard;
import com.tissue.feature.workflow.domain.service.TransitionGuardRegistry;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class IssueTransitionService implements IssueTransitionUseCase {

    private final IssueFinder issueFinder;
    private final WorkflowFinder workflowFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueValidator issueValidator;
    private final TransitionGuardRegistry guardRegistry;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void performTransition(IssueIdentifier issueIdentifier, Long transitionId, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectBy(issueIdentifier.workspaceKey(), issueIdentifier.issueKey());

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                issueIdentifier.workspaceKey(), issue.getProjectKey(), actorMemberId);

        WorkflowState oldState = issue.getCurrentState();

        WorkflowTransition transition = executeTransition(
                issue, oldState.getWorkflow().getId(), transitionId, issueIdentifier.workspaceKey(), actorMemberId);

        log.info(
                "[TRANSITION_SUCCESS] {}: {} -> {}, issueKey: {}, actorMemberId: {}",
                transition.getDisplayName(),
                issue.getCurrentState().getDisplayName(),
                transition.getTargetState().getDisplayName(),
                issue.getKey(),
                actorMemberId);

        eventPublisher.publishTransitioned(issue, transition, oldState, actor);
    }

    @Override
    public void performTransitionBySystem(
            String issueKey,
            Long transitionId,
            String workspaceKey,
            String projectKey,
            PerformSystemTransitionCommand cmd) {

        Issue issue = issueFinder.getWithProjectBy(workspaceKey, issueKey);

        WorkflowState oldState = issue.getCurrentState();
        // spotless:off
        WorkflowTransition transition =
                executeTransition(
                    issue,
                    oldState.getWorkflow().getId(),
                    transitionId,
                    workspaceKey,
                    null);
        // spotless:on

        log.info(
                "[SYSTEM_TRANSITION_SUCCESS] {}: {} -> {}, issueKey: {}, vcs email: {}, vcs username: {}",
                transition.getDisplayName(),
                issue.getCurrentState().getDisplayName(),
                transition.getTargetState().getDisplayName(),
                issue.getKey(),
                cmd.vcsUserEmail(),
                cmd.vcsUserName());

        eventPublisher.publishTransitionedBySystem(
                issue,
                transition,
                oldState,
                cmd.vcsProvider(),
                cmd.vcsUserEmail(),
                cmd.vcsUserName(),
                cmd.triggerReason() != null ? cmd.triggerReason() : "");
    }

    private WorkflowTransition executeTransition(
            Issue issue, Long workflowId, Long transitionId, String workspaceKey, @Nullable Long actorMemberId) {

        WorkflowTransition transition = workflowFinder.getTransitionWithHierarchyBy(
                workspaceKey, issue.getProjectKey(), workflowId, transitionId);

        issueValidator.ensureValidTransition(issue, workspaceKey, transition);

        executeGuards(issue, transition, actorMemberId);
        issue.transitionTo(transition.getTargetState());

        return transition;
    }

    private void executeGuards(Issue issue, WorkflowTransition transition, @Nullable Long actorMemberId) {
        // TODO: How should i prevent N+1?
        List<TransitionGuardConfig> configs = transition.getGuardConfigs();

        if (configs.isEmpty()) {
            return;
        }

        log.debug("Evaluating {} guards for transition: {}", configs.size(), transition.getDisplayName());

        for (TransitionGuardConfig config : configs) {
            TransitionGuard guard = guardRegistry.getGuard(config.getGuardType());

            GuardContext context = GuardContext.builder()
                    .issue(issue)
                    .transition(transition)
                    .workspaceKey(issue.getWorkspaceKey())
                    .projectKey(issue.getProjectKey())
                    .actorMemberId(actorMemberId)
                    .params(config.getGuardParams())
                    .build();

            guard.evaluate(context);
        }
    }
}
