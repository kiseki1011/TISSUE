package com.tissue.workflow.domain.exception;

import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.exception.ErrorContextKeys.TRANSITION_ID;
import static com.tissue.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.exception.base.ResourceNotFoundException;

public class WorkflowTransitionNotFoundException extends ResourceNotFoundException {

    public WorkflowTransitionNotFoundException(String projectKey, Long workflowId, Long transitionId) {
        super(WorkflowErrorCode.WORKFLOW_TRANSITION_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(WORKFLOW_ID, workflowId);
        addContext(TRANSITION_ID, transitionId);
    }
}
