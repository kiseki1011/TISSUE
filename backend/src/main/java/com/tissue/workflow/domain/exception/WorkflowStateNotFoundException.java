package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.STATE_ID;
import static com.tissue.global.exception.ContextKeys.WORKFLOW_ID;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class WorkflowStateNotFoundException extends ResourceNotFoundException {

    public WorkflowStateNotFoundException(Long stateId, Long workflowId) {
        super(WorkflowErrorCode.WORKFLOW_STATE_NOT_FOUND);
        addContext(STATE_ID, stateId);
        addContext(WORKFLOW_ID, workflowId);
    }
}
