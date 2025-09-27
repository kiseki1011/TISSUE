package com.tissue.api.issue.workflow.application.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;
import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.issue.workflow.domain.model.WorkflowStatus;
import com.tissue.api.issue.workflow.domain.model.WorkflowTransition;
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
	private final WorkflowRepository workflowRepository;
	private final WorkflowValidator workflowValidator;

	@Transactional
	public WorkflowResponse createWorkflow(CreateWorkflowCommand cmd) {

		workflowValidator.validateCommand(cmd);
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceCode());

		try {
			Workflow workflow = workflowRepository.save(
				Workflow.create(workspace, cmd.label(), cmd.description())
			);

			// Step mapping using tempKey
			Map<String, WorkflowStatus> statusMap = new HashMap<>();
			for (CreateWorkflowCommand.StatusCommand s : cmd.statuses()) {
				WorkflowStatus status = WorkflowStatus.create(workflow, s.label(), s.description(), s.isInitial(),
					s.isFinal());

				workflow.addStatus(status);
				statusMap.put(s.tempKey(), status);
			}

			for (CreateWorkflowCommand.TransitionCommand t : cmd.transitions()) {
				WorkflowStatus sourceStatus = statusMap.get(t.sourceTempKey());
				WorkflowStatus targetStatus = statusMap.get(t.targetTempKey());

				WorkflowTransition transition = WorkflowTransition.create(workflow, t.label(), t.description(),
					t.isMainFlow(), sourceStatus, targetStatus);

				workflow.addTransition(transition);
			}

			return WorkflowResponse.from(workflow);
		} catch (DataIntegrityViolationException e) {
			log.info("Failed due to duplicate label.", e);
			throw new DuplicateResourceException("Duplicate label is not allowed for workflows or statuses.", e);
		}
	}
}
