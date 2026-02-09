package com.tissue.workflow.application.service.finder;

import com.tissue.workflow.application.port.out.WorkflowRepository;
import com.tissue.workflow.application.port.out.WorkflowStateRepository;
import com.tissue.workflow.application.port.out.WorkflowTransitionRepository;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workflow.domain.exception.WorkflowNotFoundException;
import com.tissue.workflow.domain.exception.WorkflowStateNotFoundException;
import com.tissue.workflow.domain.exception.WorkflowTransitionNotFoundException;
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
