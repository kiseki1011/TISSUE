package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.TRANSITION_ID;
import static com.tissue.global.exception.ContextKeys.WORKFLOW_ID;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class WorkflowTransitionNotFoundException extends ResourceNotFoundException {

    public WorkflowTransitionNotFoundException(Long transitionId, Long workflowId) {
        super(WorkflowErrorCode.WORKFLOW_TRANSITION_NOT_FOUND);
        addContext(TRANSITION_ID, transitionId);
        addContext(WORKFLOW_ID, workflowId);
    }
}
