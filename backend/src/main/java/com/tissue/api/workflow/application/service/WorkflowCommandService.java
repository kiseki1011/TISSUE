package com.tissue.api.workflow.application.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.api.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.api.workflow.application.dto.request.DeleteWorkflowCommand;
import com.tissue.api.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.api.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.api.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.api.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.api.workflow.application.port.in.WorkflowCommandUseCase;
import com.tissue.api.workflow.application.port.out.WorkflowRepository;
import com.tissue.api.workflow.application.service.finder.WorkflowFinder;
import com.tissue.api.workflow.application.service.validator.WorkflowGraphValidator;
import com.tissue.api.workflow.application.service.validator.WorkflowValidator;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;
import com.tissue.api.workflow.domain.exception.DuplicateWorkflowException;
import com.tissue.api.workflow.domain.exception.TransitionNotFoundException;
import com.tissue.api.workflow.domain.guard.GuardType;
import com.tissue.api.workflow.domain.guard.TransitionGuard;
import com.tissue.api.workflow.domain.service.TransitionGuardRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowCommandService implements WorkflowCommandUseCase {

	private final ProjectFinder projectFinder;
	private final WorkflowFinder workflowFinder;
	private final WorkflowRepository workflowRepository;
	private final WorkflowValidator workflowValidator;
	private final WorkflowGraphValidator graphValidator;
	private final TransitionGuardRegistry guardRegistry;

	@Override
	public WorkflowCreateResponse create(CreateWorkflowCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());

		workflowValidator.ensureLabelUnique(project, cmd.label());

		try {
			Workflow workflow = workflowRepository.save(
				Workflow.create(project, cmd.label(), cmd.description(), cmd.color())
			);

			Map<String, WorkflowState> stateByTempKey = new HashMap<>();
			for (var s : cmd.stateDefinitions()) {
				WorkflowState state = workflow.addState(
					s.label(),
					s.description(),
					s.color(),
					s.category()
				);
				stateByTempKey.put(s.stateRef().tempKey(), state);
			}

			for (var t : cmd.transitionDefinitions()) {
				WorkflowState source = stateByTempKey.get(t.sourceStateRef().tempKey());
				WorkflowState target = stateByTempKey.get(t.targetStateRef().tempKey());

				workflow.addTransition(t.label(), t.description(), source, target);
			}

			graphValidator.ensureValidWorkflowGraph(workflow);

			return WorkflowCreateResponse.from(workflow);
		} catch (DataIntegrityViolationException e) {
			throw new DuplicateWorkflowException(cmd.label().getDisplay(), project);
		}
	}

	@Override
	public void update(UpdateWorkflowCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(cmd.workflowId(), project);

		Patchers.apply(cmd.label(), newLabel -> {
			if (!workflow.getLabel().equals(newLabel)) {
				workflowValidator.ensureLabelUnique(project, newLabel);
				workflow.rename(newLabel);
			}
		});
		Patchers.apply(cmd.description(), workflow::updateDescription);
		Patchers.apply(cmd.color(), workflow::updateColor);
	}

	@Override
	public void delete(DeleteWorkflowCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(cmd.workflowId(), project);

		// TODO: archive(soft-delete) 정책 정하기
		//  - 정책1: 해당 워크플로우를 사용하는 이슈가 단 하나라도 존재한다면 불가
		//  - 정책2: 해당 워크플로우를 사용하는 이슈가 있더라도, 전부 category가 DONE이라면 허용
		//    UI에서 해당 DONE 상태의 이슈들의 state는 회색으로 변경(disable 되었다는 표시)
		// workflowValidator.ensureDeletable();

		workflow.softDelete();
	}

	@Override
	public void updateState(UpdateStateCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(cmd.workflowId(), project);
		WorkflowState state = workflowFinder.findStateBy(cmd.stateId(), workflow);

		Patchers.apply(cmd.label(), l -> workflow.renameState(state, l));
		Patchers.apply(cmd.description(), state::updateDescription);
		Patchers.apply(cmd.color(), state::updateColor);
	}

	@Override
	public void updateTransition(UpdateTransitionCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(cmd.workflowId(), project);
		WorkflowTransition transition = workflowFinder.findTransitionBy(cmd.transitionId(), workflow);

		Patchers.apply(cmd.label(), l -> workflow.renameTransition(transition, l));
		Patchers.apply(cmd.description(), transition::updateDescription);
	}

	@Override
	public void configureTransitionGuards(ConfigureTransitionGuardsCommand cmd) {
		Project project = projectFinder.findForCommand(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(cmd.workflowId(), project);

		WorkflowTransition transition = workflow.getTransitions().stream()
			.filter(t -> t.getId().equals(cmd.transitionId()))
			.findFirst()
			.orElseThrow(() -> new TransitionNotFoundException(cmd.transitionId(), workflow.getId()));

		workflow.clearGuardsForTransition(transition);

		Set<GuardType> usedTypes = new HashSet<>();

		for (var g : cmd.guards()) {
			guardRegistry.ensureGuardExists(g.guardType());
			workflowValidator.ensureNoDuplicateGuard(g, usedTypes);

			TransitionGuard guardImplementation = guardRegistry.getGuard(g.guardType());
			guardImplementation.validateParams(g.params());

			workflow.addTransitionGuard(transition, g.guardType(), g.params(), g.order());
		}
	}
}
