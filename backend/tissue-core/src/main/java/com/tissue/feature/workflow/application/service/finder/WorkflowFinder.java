package com.tissue.feature.workflow.application.service.finder;

import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.port.repository.WorkflowStateRepository;
import com.tissue.feature.workflow.application.port.repository.WorkflowTransitionRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.exception.WorkflowNotFoundException;
import com.tissue.feature.workflow.domain.exception.WorkflowStateNotFoundException;
import com.tissue.feature.workflow.domain.exception.WorkflowTransitionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowFinder {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStateRepository stateRepository;
    private final WorkflowTransitionRepository transitionRepository;

    public Workflow getById(Long workflowId) {
        return workflowRepository.findById(workflowId).orElseThrow(() -> new WorkflowNotFoundException(workflowId));
    }

    // workflowId/stateId/transitionId are globally unique.
    public WorkflowState getStateWithHierarchyBy(Long workflowId, Long stateId) {
        return stateRepository
                .findStateWithHierarchyByWorkflowIdAndId(workflowId, stateId)
                .orElseThrow(() -> new WorkflowStateNotFoundException(workflowId, stateId));
    }

    public WorkflowTransition getTransitionWithHierarchyBy(Long workflowId, Long transitionId) {
        return transitionRepository
                .findTransitionWithHierarchyByWorkflowIdAndId(workflowId, transitionId)
                .orElseThrow(() -> new WorkflowTransitionNotFoundException(workflowId, transitionId));
    }
}
