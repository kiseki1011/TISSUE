package com.tissue.api.workflow.application.service.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.api.workflow.application.port.out.WorkflowStateRepository;
import com.tissue.api.workflow.application.port.out.WorkflowTransitionRepository;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;
import com.tissue.api.workflow.domain.exception.StateNotFoundException;
import com.tissue.api.workflow.domain.exception.TransitionNotFoundException;
import com.tissue.api.workflow.domain.exception.WorkflowNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowFinder {

	private final WorkflowQueryRepository workflowQueryRepo;
	private final WorkflowStateRepository statusRepo;
	private final WorkflowTransitionRepository transitionRepo;

	public Workflow findBy(Long id) {
		return workflowQueryRepo.findById(id)
			.orElseThrow(() -> new WorkflowNotFoundException(id));
	}

	public Workflow findBy(Long id, Project project) {
		return workflowQueryRepo.findByIdAndProject(id, project)
			.orElseThrow(() -> new WorkflowNotFoundException(id, project.getKey(), project.getWorkspaceKey()));
	}

	public WorkflowState findStateBy(Long id, Workflow workflow) {
		return statusRepo.findByIdAndWorkflow(id, workflow)
			.orElseThrow(() -> new StateNotFoundException(id, workflow.getId()));
	}

	public WorkflowTransition findTransitionBy(Long id, Workflow workflow) {
		return transitionRepo.findByIdAndWorkflow(id, workflow)
			.orElseThrow(() -> new TransitionNotFoundException(id, workflow.getId()));
	}
}
