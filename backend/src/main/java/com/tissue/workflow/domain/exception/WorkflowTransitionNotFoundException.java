package com.tissue.workflow.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.TRANSITION_ID;
import static com.tissue.common.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class WorkflowTransitionNotFoundException extends ResourceNotFoundException {

    public WorkflowTransitionNotFoundException(Long transitionId, Long workflowId) {
        super(WorkflowErrorCode.WORKFLOW_TRANSITION_NOT_FOUND);
        addContext(TRANSITION_ID, transitionId);
        addContext(WORKFLOW_ID, workflowId);
    }
}
