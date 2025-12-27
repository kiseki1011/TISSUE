package com.tissue.workflow.application.service.finder;

import org.springframework.stereotype.Component;

import com.tissue.project.domain.Project;
import com.tissue.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.workflow.application.port.out.WorkflowStateRepository;
import com.tissue.workflow.application.port.out.WorkflowTransitionRepository;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workflow.domain.exception.WorkflowExceptions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowFinder {

	private final WorkflowQueryRepository workflowQueryRepo;
	private final WorkflowStateRepository statusRepo;
	private final WorkflowTransitionRepository transitionRepo;

	public Workflow findBy(Long id, Project project) {
		return workflowQueryRepo.findByIdAndProject(id, project)
			.orElseThrow(() -> WorkflowExceptions.notFound(id, project.getKey()));
	}

	public WorkflowState findStateBy(Long id, Workflow workflow) {
		return statusRepo.findByIdAndWorkflow(id, workflow)
			.orElseThrow(() -> WorkflowExceptions.stateNotFound(id, workflow.getId()));
	}

	public WorkflowTransition findTransitionBy(Long id, Workflow workflow) {
		return transitionRepo.findByIdAndWorkflow(id, workflow)
			.orElseThrow(() -> WorkflowExceptions.transitionNotFound(id, workflow.getId()));
	}
}
