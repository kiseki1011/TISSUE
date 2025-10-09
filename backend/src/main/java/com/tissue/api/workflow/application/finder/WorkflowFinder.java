package com.tissue.api.workflow.application.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workflow.domain.model.WorkflowStatus;
import com.tissue.api.workflow.domain.model.WorkflowTransition;
import com.tissue.api.workflow.repository.WorkflowRepository;
import com.tissue.api.workflow.repository.WorkflowStatusRepository;
import com.tissue.api.workflow.repository.WorkflowTransitionRepository;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowFinder {

	private final WorkflowRepository workflowRepo;
	private final WorkflowStatusRepository statusRepo;
	private final WorkflowTransitionRepository transitionRepo;

	public Workflow findWorkflow(Workspace workspace, Long id) {
		return workflowRepo.findByWorkspaceAndId(workspace, id)
			.orElseThrow(() -> new ResourceNotFoundException(
				"Workflow not found: workspaceKey=" + workspace.getKey() + ", workflowId=" + id));
	}

	public WorkflowStatus findWorkflowStatus(Workflow workflow, Long id) {
		return statusRepo.findByWorkflowAndId(workflow, id)
			.orElseThrow(() -> new ResourceNotFoundException(
				"Workflow status not found: workflowId=" + workflow.getId() + ", statusId=" + id));
	}

	public WorkflowTransition findWorkflowTransition(Workflow workflow, Long id) {
		return transitionRepo.findByWorkflowAndId(workflow, id)
			.orElseThrow(() -> new ResourceNotFoundException(
				"Workflow transition not found: workflowId=" + workflow.getId() + ", transitionId=" + id));
	}
}
