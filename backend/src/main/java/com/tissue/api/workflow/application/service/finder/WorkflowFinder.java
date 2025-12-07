package com.tissue.api.workflow.application.service.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.application.port.out.WorkflowRepository;
import com.tissue.api.workflow.application.port.out.WorkflowStateRepository;
import com.tissue.api.workflow.application.port.out.WorkflowTransitionRepository;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowFinder {

	private final WorkflowRepository workflowRepo;
	private final WorkflowStateRepository statusRepo;
	private final WorkflowTransitionRepository transitionRepo;

	public Workflow findBy(Project project, Long id) {
		return workflowRepo.findByProjectAndId(project, id)
			// TODO: WorkflowNotFoundException
			.orElseThrow(() -> new RuntimeException(
				"Workflow not found: workspaceKey=" + project.getKey() + ", workflowId=" + id));
	}

	public WorkflowState findStateBy(Workflow workflow, Long id) {
		return statusRepo.findByWorkflowAndId(workflow, id)
			// TODO: WorkflowStateNotFoundException
			.orElseThrow(() -> new RuntimeException(
				"Workflow status not found: workflowId=" + workflow.getId() + ", statusId=" + id));
	}

	public WorkflowTransition findTransitionBy(Workflow workflow, Long id) {
		return transitionRepo.findByWorkflowAndId(workflow, id)
			// TODO: WorkflowTransitionNotFoundException
			.orElseThrow(() -> new RuntimeException(
				"Workflow transition not found: workflowId=" + workflow.getId() + ", transitionId=" + id));
	}
}
