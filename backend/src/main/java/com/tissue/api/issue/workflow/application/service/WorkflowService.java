package com.tissue.api.issue.workflow.application.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;
import com.tissue.api.issue.workflow.application.finder.WorkflowFinder;
import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.issue.workflow.domain.model.WorkflowStatus;
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

	@Transactional
	public WorkflowResponse create(CreateWorkflowCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());

		workflowValidator.ensureLabelUnique(workspace, cmd.label());
		graphValidator.validateWorkflowGraphStructure(
			cmd.statusCommands().stream().map(s -> s.toValidationData()).toList(),
			cmd.transitionCommands().stream().map(t -> t.toValidationData()).toList()
		);

		try {
			Workflow workflow = workflowRepository.save(Workflow.create(workspace, cmd.label(), cmd.description()));

			Map<String, WorkflowStatus> statusMap = new HashMap<>();
			for (CreateWorkflowCommand.StatusCommand s : cmd.statusCommands()) {
				WorkflowStatus status = workflow.addStatus(s.label(), s.description(), s.initial(), s.terminal());
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

	// @Transactional
	// public WorkflowResponse patch(PatchWorkflowCommand cmd) {
	// 	Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());
	// 	Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.id());
	//
	// 	// TODO: Workflow의 label/description 수정
	// 	//   - Label 중복 검증 로직 필요(WorkflowValidator에 만들기, Workspace 스코프 내에서 유일해야 함)
	//
	// 	if (!workflow.getLabel().equals(cmd.label())) {
	// 		validator.ensureUniqueLabel(ws, cmd.label());
	// 		Patchers.apply(cmd.label(), workflow::rename);
	// 	}
	//
	// 	Patchers.apply(cmd.description(), workflow::updateDescription);
	// 	Patchers.apply(cmd.color(), workflow::updateColor);
	//
	// 	return WorkflowResponse.from(workflow);
	// }
	//
	// @Transactional
	// public WorkflowResponse softDelete(DeleteWorkflowCommand cmd) {
	// 	Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());
	// 	Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.id());
	//
	// 	// TODO: Workflow 삭제(soft-delete)
	// 	//  - 하나 이상의 IssueType이 Workflow를 사용 중이면 막기(ensureDeletable 구현)
	// 	//  - 이미 엔티티에서 soft-delete가 WorkflowStatus, WorkflowTransition에 전파되도록 구현
	// 	//  - systemType(기본으로 제공하는 Workflow에 해당하면)이면 삭제를 막을까?
	// 	// workflowValidator.ensureDeletable();
	//
	// 	workflow.softDelete();
	//
	// 	return WorkflowResponse.from(workflow);
	// }
	//
	// @Transactional
	// public WorkflowResponse patchStatus(PatchStatusCommand cmd) {
	// 	Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());
	// 	Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowId());
	// 	WorkflowStatus status = workflowFinder.findWorkflowStatus(workflow, cmd.statusId());
	//
	// 	Patchers.apply(cmd.label(), l -> workflow.renameStatus(status, l));
	// 	Patchers.apply(cmd.description(), d -> workflow.updateStatusDescription(status, d));
	// 	Patchers.apply(cmd.color(), c -> workflow.updateStatusColor(status, c));
	//
	// 	return WorkflowResponse.from(workflow);
	// }
	//
	// @Transactional
	// public WorkflowResponse patchTransition(PatchTransitionCommand cmd) {
	// 	Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());
	// 	Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowId());
	// 	WorkflowTransition transition = workflowFinder.findWorkflowTransition(workflow, cmd.transitionId());
	//
	// 	Patchers.apply(cmd.label(), l -> workflow.renameTransition(transition, l));
	// 	Patchers.apply(cmd.description(), d -> workflow.updateTransitionDescription(transition, d));
	// 	Patchers.apply(cmd.color(), c -> workflow.updateTransitionColor(transition, c));
	//
	// 	return WorkflowResponse.from(workflow);
	// }
}
