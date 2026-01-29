package com.tissue.workflow.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.STATE_ID;
import static com.tissue.common.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class WorkflowStateNotFoundException extends ResourceNotFoundException {

    public WorkflowStateNotFoundException(Long stateId, Long workflowId) {
        super(WorkflowErrorCode.WORKFLOW_STATE_NOT_FOUND);
        addContext(STATE_ID, stateId);
        addContext(WORKFLOW_ID, workflowId);
    }
}
