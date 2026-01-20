package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.issue.application.service.event.IssueEventPublisher;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.application.service.validator.IssueValidator;
import com.tissue.issue.domain.Issue;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.workflow.application.service.finder.WorkflowFinder;
import com.tissue.workflow.domain.TransitionGuardConfig;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workflow.domain.guard.GuardContext;
import com.tissue.workflow.domain.guard.TransitionGuard;
import com.tissue.workflow.domain.service.TransitionGuardRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueTransitionService implements IssueTransitionUseCase {

    private final IssueFinder issueFinder;
    private final ProjectFinder projectFinder;
    private final WorkflowFinder workflowFinder;
    private final IssueValidator issueValidator;
    private final TransitionGuardRegistry guardRegistry;
    private final IssueEventPublisher eventPublisher;
    private final IssueAuthorizationService issueAuthService;

    @Override
    @Transactional
    public void performTransition(PerformTransitionCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);

        issueAuthService.requireIssueEditPermission(issue, actorContext);

        Workflow workflow = issue.getIssueType().getWorkflow();
        WorkflowTransition transition = workflowFinder.getTransitionBy(cmd.transitionId(), workflow);

        WorkflowState oldState = issue.getCurrentState();

        issueValidator.ensureValidTransition(issue, cmd.transitionId(), actorContext.workspaceKey(), transition);

        executeGuards(
                actorContext.workspaceKey(), actorContext.projectKey(), issue, transition, actorContext.memberId());

        issue.transitionTo(transition.getTargetState());

        log.info(
                "[TRANSITION SUCCESS] {}: {} -> {}, issueKey: {}, actorMemberId: {}",
                transition.getDisplayName(),
                issue.getCurrentState().getDisplayName(),
                transition.getTargetState().getDisplayName(),
                issue.getKey(),
                actorContext.memberId());

        eventPublisher.publishTransitioned(issue, transition, oldState, actorContext);
    }

    private void executeGuards(
            String workspaceKey, String projectKey, Issue issue, WorkflowTransition transition, Long actorMemberId) {
        // TODO: how should i prevent N+1? get guardConfigs with JOIN FETCH?
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
                    .workspaceKey(workspaceKey)
                    .projectKey(projectKey)
                    .actorMemberId(actorMemberId)
                    .params(config.getGuardParams())
                    .build();

            guard.evaluate(context);
        }
    }
}
