package com.tissue.api.issue.application.service;

import static com.tissue.api.common.util.IssueKeyUtil.*;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.api.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.api.issue.application.service.finder.IssueFinder;
import com.tissue.api.issue.application.service.validator.IssueValidator;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.event.IssueTransitionedEvent;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.application.service.finder.ProjectMemberFinder;
import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.ProjectMember;
import com.tissue.api.workflow.application.service.finder.WorkflowFinder;
import com.tissue.api.workflow.domain.TransitionGuardConfig;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;
import com.tissue.api.workflow.domain.exception.TransitionGuardFailedException;
import com.tissue.api.workflow.domain.guard.GuardContext;
import com.tissue.api.workflow.domain.guard.TransitionGuard;
import com.tissue.api.workflow.domain.guard.TransitionGuardRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueTransitionService implements IssueTransitionUseCase {

	private final IssueFinder issueFinder;
	private final ProjectFinder projectFinder;
	private final ProjectMemberFinder projectMemberFinder;
	private final WorkflowFinder workflowFinder;
	private final IssueValidator issueValidator;
	private final TransitionGuardRegistry guardRegistry;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public void performTransition(PerformTransitionCommand cmd) {
		Project project = projectFinder.findForCommand(extractProjectKey(cmd.projectKey()), cmd.workspaceKey());
		Issue issue = issueFinder.findBy(cmd.issueKey(), project);
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

		Workflow workflow = issue.getIssueType().getWorkflow();
		WorkflowTransition transition = workflowFinder.findTransitionBy(cmd.transitionId(), workflow);

		WorkflowState oldState = issue.getCurrentState();

		issueValidator.ensureValidTransition(issue, cmd.transitionId(), cmd.workspaceKey(), transition);

		executeGuards(cmd.workspaceKey(), cmd.projectKey(), issue, transition, cmd.actorMemberId());

		issue.transitionTo(transition.getTargetState());

		log.info("[TRANSITION SUCCESS] {}: {} -> {}, issueKey: {}, actorMemberId: {}",
			transition.getDisplayLabel(),
			issue.getCurrentState().getDisplayLabel(),
			transition.getTargetState().getDisplayLabel(),
			issue.getKey(),
			cmd.actorMemberId()
		);

		eventPublisher.publishEvent(IssueTransitionedEvent.create(issue, transition, oldState, actor));
	}

	private void executeGuards(
		String workspaceKey,
		String projectKey,
		Issue issue,
		WorkflowTransition transition,
		Long actorMemberId
	) {
		// TODO: guardConfigs를 JOIN FETCH로 가져와서 N+1 방지
		List<TransitionGuardConfig> configs = transition.getGuardConfigs();

		if (configs.isEmpty()) {
			return;
		}

		log.debug("Evaluating {} guards for transition: {}", configs.size(), transition.getDisplayLabel());

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

			boolean failEvaluation = !guard.evaluate(context);
			if (failEvaluation) {
				String failReason = guard.getFailureMessage(context);

				log.info("[GUARD FAILED] guardType: {}, issueKey: {}, reason: {}",
					guard.getType(), issue.getKey(), failReason);

				throw new TransitionGuardFailedException(guard.getType(), failReason, issue.getKey(), workspaceKey);
			}
		}
	}
}
