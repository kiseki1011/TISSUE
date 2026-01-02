package com.tissue.workflow.application.service.finder;

import com.tissue.project.domain.Project;
import com.tissue.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.workflow.application.port.out.WorkflowStateRepository;
import com.tissue.workflow.application.port.out.WorkflowTransitionRepository;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workflow.domain.exception.WorkflowExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowFinder {

    private final WorkflowQueryRepository workflowQueryRepo;
    private final WorkflowStateRepository statusRepo;
    private final WorkflowTransitionRepository transitionRepo;

    // TODO: find -> get
    public Workflow findBy(Long id, Project project) {
        return workflowQueryRepo
                .findByIdAndProject(id, project)
                .orElseThrow(() -> WorkflowExceptions.notFound(id, project.getKey()));
    }

    // TODO: find -> get
    public WorkflowState findStateBy(Long id, Workflow workflow) {
        return statusRepo
                .findByIdAndWorkflow(id, workflow)
                .orElseThrow(() -> WorkflowExceptions.stateNotFound(id, workflow.getId()));
    }

    // TODO: find -> get
    public WorkflowTransition findTransitionBy(Long id, Workflow workflow) {
        return transitionRepo
                .findByIdAndWorkflow(id, workflow)
                .orElseThrow(() -> WorkflowExceptions.transitionNotFound(id, workflow.getId()));
    }
}
