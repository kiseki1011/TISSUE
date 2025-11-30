package com.tissue.api.workflow.application.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.api.common.util.Patchers;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.application.GuardConfigData;
import com.tissue.api.workflow.application.dto.ConfigureTransitionGuardsCommand;
import com.tissue.api.workflow.application.dto.CreateWorkflowCommand;
import com.tissue.api.workflow.application.dto.DeleteWorkflowCommand;
import com.tissue.api.workflow.application.dto.PatchWorkflowCommand;
import com.tissue.api.workflow.application.dto.UpdateStateCommand;
import com.tissue.api.workflow.application.dto.UpdateTransitionCommand;
import com.tissue.api.workflow.application.finder.WorkflowFinder;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;
import com.tissue.api.workflow.domain.gaurd.GuardType;
import com.tissue.api.workflow.domain.service.WorkflowGraphValidator;
import com.tissue.api.workflow.domain.service.WorkflowValidator;
import com.tissue.api.workflow.presentation.dto.response.WorkflowResponse;
import com.tissue.api.workflow.repository.WorkflowRepository;
import com.tissue.api.workspace.application.service.finder.WorkspaceFinder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

	private final WorkspaceFinder workspaceFinder;
	private final ProjectFinder projectFinder;
	private final WorkflowFinder workflowFinder;
	private final WorkflowRepository workflowRepository;
	private final WorkflowValidator workflowValidator;
	private final WorkflowGraphValidator graphValidator;
	private final TransitionGuardRegistry guardRegistry;

	// TODO: spring-retry 적용
	@Transactional
	public WorkflowResponse create(CreateWorkflowCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());

		workflowValidator.ensureLabelUnique(project, cmd.label());
		graphValidator.validateWorkflowGraphStructure(
			cmd.stateCommands().stream().map(s -> s.toValidationData()).toList(),
			cmd.transitionCommands().stream().map(t -> t.toValidationData()).toList()
		);

		try {
			Workflow workflow = workflowRepository.save(
				Workflow.create(project, cmd.label(), cmd.description(), cmd.color())
			);

			Map<String, WorkflowState> stateByTempKey = new HashMap<>();
			for (CreateWorkflowCommand.StateCommand s : cmd.stateCommands()) {
				WorkflowState status = workflow.addState(
					s.label(),
					s.description(),
					s.color(),
					s.initial(),
					s.terminal()
				);
				stateByTempKey.put(s.ref().tempKey(), status);
			}

			for (CreateWorkflowCommand.TransitionCommand t : cmd.transitionCommands()) {
				WorkflowState source = stateByTempKey.get(t.sourceRef().tempKey());
				WorkflowState target = stateByTempKey.get(t.targetRef().tempKey());

				workflow.addTransition(t.label(), t.description(), source, target);
			}

			graphValidator.ensureValidWorkflowGraph(workflow);

			return WorkflowResponse.from(workflow);

		} catch (DataIntegrityViolationException e) {
			log.info("Failed due to duplicate label.", e);
			// TODO: DuplicateWorkflowException vs DuplicateWorkflowLabelException
			throw new RuntimeException("Duplicate label is not allowed.", e);
		}
	}

	@Transactional
	public WorkflowResponse patch(PatchWorkflowCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(project, cmd.id());

		Patchers.apply(cmd.label(), newLabel -> {
			if (!workflow.getLabel().equals(newLabel)) {
				workflowValidator.ensureLabelUnique(project, newLabel);
				workflow.rename(newLabel);
			}
		});

		Patchers.apply(cmd.description(), workflow::updateDescription);
		Patchers.apply(cmd.color(), workflow::updateColor);

		return WorkflowResponse.from(workflow);
	}

	@Transactional
	public WorkflowResponse softDelete(DeleteWorkflowCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(project, cmd.id());

		// TODO: Workflow의 softDelete 주석 참고
		// workflowValidator.ensureDeletable();

		workflow.delete();

		return WorkflowResponse.from(workflow);
	}

	@Transactional
	public WorkflowResponse updateState(UpdateStateCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(project, cmd.workflowId());
		WorkflowState state = workflowFinder.findStateBy(workflow, cmd.statusId());

		Patchers.apply(cmd.label(), l -> workflow.renameState(state, l));
		Patchers.apply(cmd.description(), state::updateDescription);
		Patchers.apply(cmd.color(), state::updateColor);

		return WorkflowResponse.from(workflow);
	}

	@Transactional
	public WorkflowResponse updateTransition(UpdateTransitionCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(project, cmd.workflowId());
		WorkflowTransition transition = workflowFinder.findTransitionBy(workflow, cmd.transitionId());

		Patchers.apply(cmd.label(), l -> workflow.renameTransition(transition, l));
		Patchers.apply(cmd.description(), transition::updateDescription);

		return WorkflowResponse.from(workflow);
	}

	@Transactional
	public void configureTransitionGuards(ConfigureTransitionGuardsCommand cmd) {

		// Workflow와 Transition 조회
		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(project, cmd.workflowId());

		WorkflowTransition transition = workflow.getTransitions().stream()
			.filter(t -> t.getId().equals(cmd.transitionId()))
			.findFirst()
			// TODO: TransitionNotFoundException vs WorkflowTransitionNotFoundException
			.orElseThrow(() -> new RuntimeException("Transition not found"));

		workflow.clearGuardsForTransition(transition);

		Set<GuardType> usedTypes = new HashSet<>();

		for (var g : cmd.guards()) {
			guardRegistry.getGuard(g.guardType());
			ensureNoDuplicateGuard(g, usedTypes);

			String paramsJson = serializeParams(g);

			workflow.addTransitionGuard(transition, g.guardType(), paramsJson, g.order());
		}
	}

	private String serializeParams(GuardConfigData guardConfigData) {
		String paramsJson = null;
		if (guardConfigData.params() != null && !guardConfigData.params().isEmpty()) {
			try {
				paramsJson = new ObjectMapper().writeValueAsString(guardConfigData.params());
			} catch (JsonProcessingException e) {
				// TODO: IllegalStateException vs IllegalArgumentException
				throw new IllegalArgumentException("Invalid guard parameters");
			}
		}
		return paramsJson;
	}

	private void ensureNoDuplicateGuard(GuardConfigData g, Set<GuardType> usedTypes) {
		boolean dup = !usedTypes.add(g.guardType());
		if (dup) {
			// TODO: DuplicateGuardTypeException vs IllegalArgumentException vs IllegalStateException
			//  예외를 던지지 말고 이 로직을 반복문 안으로 옮기고, 중복된 가드 타입이 있다면 continue하는 식으로 구현할까?
			throw new RuntimeException("Duplicate guard type: " + g.guardType());
		}
	}
}
