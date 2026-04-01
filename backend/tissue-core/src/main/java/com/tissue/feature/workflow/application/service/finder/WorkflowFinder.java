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

    public Workflow getWithProjectBy(String workspaceKey, Long workflowId) {
        return workflowRepository
                .findWithProjectByWorkspaceKeyAndId(workspaceKey, workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
    }

    public WorkflowState getStateWithHierarchyBy(String workspaceKey, Long workflowId, Long stateId) {
        return stateRepository
                .findStateWithHierarchyByWorkspaceKeyAndWorkflowIdAndId(workspaceKey, workflowId, stateId)
                .orElseThrow(() -> new WorkflowStateNotFoundException(workflowId, stateId));
    }

    public WorkflowTransition getTransitionWithHierarchyBy(String workspaceKey, Long workflowId, Long transitionId) {
        return transitionRepository
                .findTransitionWithHierarchyByWorkspaceKeyAndWorkflowIdAndId(workspaceKey, workflowId, transitionId)
                .orElseThrow(() -> new WorkflowTransitionNotFoundException(workflowId, transitionId));
    }
}
