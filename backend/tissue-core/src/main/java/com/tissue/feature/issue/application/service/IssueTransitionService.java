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
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.service.TransitionGuardEvaluator;
import com.tissue.shared.dto.IssueIdentifier;
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
    private final TransitionGuardEvaluator guardEvaluator;
    private final IssueEventPublisher eventPublisher;

    @Override
    public void performTransition(IssueIdentifier iid, Long transitionId, Long actorMemberId) {
        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId);

        WorkflowState oldState = issue.getCurrentState();

        WorkflowTransition transition =
                executeTransition(issue, oldState.getWorkflow().getId(), transitionId, actorMemberId);

        log.info(
                "Transition success {}: {} -> {}, issueKey: {}, actorMemberId: {}",
                transition.getDisplayName(),
                oldState.getDisplayName(),
                transition.getTargetState().getDisplayName(),
                issue.getKey(),
                actorMemberId);

        eventPublisher.publishTransitioned(issue, transition, oldState, actor);
    }

    @Override
    public void performTransitionBySystem(String issueKey, Long transitionId, PerformSystemTransitionCommand cmd) {
        Issue issue = issueFinder.getWithProjectByIssueKey(issueKey);

        WorkflowState oldState = issue.getCurrentState();
        WorkflowTransition transition =
                executeTransition(issue, oldState.getWorkflow().getId(), transitionId, null);

        log.info(
                "System transition success {}: {} -> {}, issueKey: {}, vcs email: {}, vcs username: {}",
                transition.getDisplayName(),
                oldState.getDisplayName(),
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
            Issue issue, Long workflowId, Long transitionId, @Nullable Long actorMemberId) {
        WorkflowTransition transition = workflowFinder.getTransitionWithHierarchyBy(workflowId, transitionId);

        issueValidator.ensureValidTransition(issue, transition);

        guardEvaluator.executeOrThrow(issue, transition, actorMemberId);

        issue.transitionTo(transition.getTargetState());

        return transition;
    }
}
