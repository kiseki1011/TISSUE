package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.issue.application.service.validator.IssueValidator;
import com.tissue.issue.domain.Issue;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.application.service.finder.WorkflowFinder;
import com.tissue.workflow.domain.TransitionGuardConfig;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workflow.domain.guard.GuardContext;
import com.tissue.workflow.domain.guard.TransitionGuard;
import com.tissue.workflow.domain.service.TransitionGuardRegistry;
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
    private final IssueValidator issueValidator;
    private final TransitionGuardRegistry guardRegistry;
    private final IssueEventPublisher eventPublisher;
    private final IssueAuthorizationService issueAuthService;

    @Override
    public void performTransition(String issueKey, Long transitionId, ProjectMemberContext actorContext) {
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), issueKey);

        WorkflowState oldState = issue.getCurrentState();

        WorkflowTransition transition = executeTransition(
                issue,
                oldState.getWorkflow().getId(),
                transitionId,
                actorContext.workspaceKey(),
                actorContext.memberId());

        log.info(
                "[TRANSITION_SUCCESS] {}: {} -> {}, issueKey: {}, actorMemberId: {}",
                transition.getDisplayName(),
                issue.getCurrentState().getDisplayName(),
                transition.getTargetState().getDisplayName(),
                issue.getKey(),
                actorContext.memberId());

        eventPublisher.publishTransitioned(issue, transition, oldState, actorContext);
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
