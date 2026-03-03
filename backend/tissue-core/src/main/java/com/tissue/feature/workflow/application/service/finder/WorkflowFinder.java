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

    public Workflow getWithProjectBy(String workspaceKey, String projectKey, Long workflowId) {
        return workflowRepository
                .findWithProjectByWorkspaceKeyAndProjectKeyAndId(workspaceKey, projectKey, workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(projectKey, workflowId));
    }

    public WorkflowState getStateWithHierarchyBy(
            String workspaceKey, String projectKey, Long workflowId, Long stateId) {
        return stateRepository
                .findStateWithHierarchyByKeys(workspaceKey, projectKey, workflowId, stateId)
                .orElseThrow(() -> new WorkflowStateNotFoundException(projectKey, workflowId, stateId));
    }

    public WorkflowTransition getTransitionWithHierarchyBy(
            String workspaceKey, String projectKey, Long workflowId, Long transitionId) {
        return transitionRepository
                .findTransitionWithHierarchyByKeys(workspaceKey, projectKey, workflowId, transitionId)
                .orElseThrow(() -> new WorkflowTransitionNotFoundException(projectKey, workflowId, transitionId));
    }
}
