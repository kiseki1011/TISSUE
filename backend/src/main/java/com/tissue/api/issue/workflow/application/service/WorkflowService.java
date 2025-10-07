package com.tissue.api.issue.workflow.application.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.workflow.application.dto.ConfigureTransitionGuardsCommand;
import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;
import com.tissue.api.issue.workflow.application.dto.PatchStatusCommand;
import com.tissue.api.issue.workflow.application.dto.PatchTransitionCommand;
import com.tissue.api.issue.workflow.application.dto.PatchWorkflowCommand;
import com.tissue.api.issue.workflow.application.finder.WorkflowFinder;
import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.issue.workflow.domain.model.WorkflowStatus;
import com.tissue.api.issue.workflow.domain.model.WorkflowTransition;
import com.tissue.api.issue.workflow.domain.service.WorkflowGraphValidator;
import com.tissue.api.issue.workflow.domain.service.WorkflowValidator;
import com.tissue.api.issue.workflow.infrastructure.repository.WorkflowRepository;
import com.tissue.api.issue.workflow.presentation.dto.response.WorkflowResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

	private final WorkspaceFinder workspaceFinder;
	private final WorkflowFinder workflowFinder;
	private final WorkflowRepository workflowRepository;
	private final WorkflowValidator workflowValidator;
	private final WorkflowGraphValidator graphValidator;
	private final TransitionGuardRegistry guardRegistry;

	@Transactional
	public WorkflowResponse create(CreateWorkflowCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());

		workflowValidator.ensureLabelUnique(workspace, cmd.label());
		graphValidator.validateWorkflowGraphStructure(
			cmd.statusCommands().stream().map(s -> s.toValidationData()).toList(),
			cmd.transitionCommands().stream().map(t -> t.toValidationData()).toList()
		);

		try {
			Workflow workflow = workflowRepository.save(
				Workflow.create(workspace, cmd.label(), cmd.description(), cmd.color())
			);

			Map<String, WorkflowStatus> statusMap = new HashMap<>();
			for (CreateWorkflowCommand.StatusCommand s : cmd.statusCommands()) {
				WorkflowStatus status = workflow.addStatus(s.label(), s.description(), s.color(), s.initial(),
					s.terminal());
				statusMap.put(s.ref().tempKey(), status);
			}

			for (CreateWorkflowCommand.TransitionCommand t : cmd.transitionCommands()) {
				WorkflowStatus src = statusMap.get(t.sourceRef().tempKey());
				WorkflowStatus trg = statusMap.get(t.targetRef().tempKey());

				workflow.addTransition(t.label(), t.description(), src, trg);
			}

			graphValidator.ensureValidWorkflowGraph(workflow);

			return WorkflowResponse.from(workflow);
		} catch (DataIntegrityViolationException e) {
			log.info("Failed due to duplicate label.", e);
			throw new DuplicateResourceException("Duplicate label is not allowed.", e);
		}
	}

	@Transactional
	public WorkflowResponse patch(PatchWorkflowCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.id());

		Patchers.apply(cmd.label(), newLabel -> {
			if (!workflow.getLabel().equals(newLabel)) {
				workflowValidator.ensureLabelUnique(workspace, newLabel);
				workflow.rename(newLabel);
			}
		});

		Patchers.apply(cmd.description(), workflow::updateDescription);
		Patchers.apply(cmd.color(), workflow::updateColor);

		return WorkflowResponse.from(workflow);
	}

	@Transactional
	public WorkflowResponse softDelete(String workspaceKey, Long id) {
		Workspace workspace = workspaceFinder.findWorkspace(workspaceKey);
		Workflow workflow = workflowFinder.findWorkflow(workspace, id);

		// TODO: Workflow의 softDelete 주석 참고
		// workflowValidator.ensureDeletable();

		workflow.softDelete();

		return WorkflowResponse.from(workflow);
	}

	@Transactional
	public WorkflowResponse patchStatus(PatchStatusCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowId());
		WorkflowStatus status = workflowFinder.findWorkflowStatus(workflow, cmd.statusId());

		Patchers.apply(cmd.label(), l -> workflow.renameStatus(status, l));
		Patchers.apply(cmd.description(), status::updateDescription);
		Patchers.apply(cmd.color(), status::updateColor);

		return WorkflowResponse.from(workflow);
	}

	@Transactional
	public WorkflowResponse patchTransition(PatchTransitionCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowId());
		WorkflowTransition transition = workflowFinder.findWorkflowTransition(workflow, cmd.transitionId());

		Patchers.apply(cmd.label(), l -> workflow.renameTransition(transition, l));
		Patchers.apply(cmd.description(), transition::updateDescription);

		return WorkflowResponse.from(workflow);
	}

	@Transactional
	public void configureTransitionGuards(
		ConfigureTransitionGuardsCommand cmd
	) {
		// Workflow와 Transition 조회
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowId());

		WorkflowTransition transition = workflow.getTransitions().stream()
			.filter(t -> t.getId().equals(cmd.transitionId()))
			.findFirst()
			.orElseThrow(() -> new ResourceNotFoundException("Transition not found"));

		// Guard 타입 검증
		for (var guardConfigData : cmd.guards()) {
			guardRegistry.getGuard(guardConfigData.guardType());  // 존재하는 Guard인지 확인
		}

		// 기존 Guard 모두 제거 후 새로 설정
		workflow.clearGaurdsForTranstion(transition);

		for (var item : cmd.guards()) {
			// JSON 직렬화
			String paramsJson = null;
			if (item.params() != null && !item.params().isEmpty()) {
				try {
					paramsJson = new ObjectMapper().writeValueAsString(item.params());
				} catch (JsonProcessingException e) {
					throw new InvalidOperationException("Invalid guard parameters");
				}
			}

			workflow.addTransitionGuard(transition, item.guardType(), paramsJson, item.order());
		}
	}
}
