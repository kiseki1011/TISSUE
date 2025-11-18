package com.tissue.api.workflow.application.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;
import com.tissue.api.workflow.repository.WorkflowRepository;
import com.tissue.api.workflow.repository.WorkflowStateRepository;
import com.tissue.api.workflow.repository.WorkflowTransitionRepository;
import com.tissue.api.workspace.domain.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowFinder {

	private final WorkflowRepository workflowRepo;
	private final WorkflowStateRepository statusRepo;
	private final WorkflowTransitionRepository transitionRepo;

	public Workflow findWorkflow(Workspace workspace, Long id) {
		return workflowRepo.findByWorkspaceAndId(workspace, id)
			// TODO: WorkflowNotFoundException
			.orElseThrow(() -> new RuntimeException(
				"Workflow not found: workspaceKey=" + workspace.getKey() + ", workflowId=" + id));
	}

	public WorkflowState findWorkflowState(Workflow workflow, Long id) {
		return statusRepo.findByWorkflowAndId(workflow, id)
			// TODO: WorkflowStateNotFoundException
			.orElseThrow(() -> new RuntimeException(
				"Workflow status not found: workflowId=" + workflow.getId() + ", statusId=" + id));
	}

	public WorkflowTransition findWorkflowTransition(Workflow workflow, Long id) {
		return transitionRepo.findByWorkflowAndId(workflow, id)
			// TODO: WorkflowTransitionNotFoundException
			.orElseThrow(() -> new RuntimeException(
				"Workflow transition not found: workflowId=" + workflow.getId() + ", transitionId=" + id));
	}
}
