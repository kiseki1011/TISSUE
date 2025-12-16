package com.tissue.issue.application.service;

import static com.tissue.common.util.IssueKeyUtil.*;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.application.service.validator.IssueValidator;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.event.IssueTransitionedEvent;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.workflow.application.service.finder.WorkflowFinder;
import com.tissue.workflow.domain.TransitionGuardConfig;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workflow.domain.guard.GuardContext;
import com.tissue.workflow.domain.guard.TransitionGuard;
import com.tissue.workflow.domain.service.TransitionGuardRegistry;

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

			guard.evaluate(context);
		}
	}
}
