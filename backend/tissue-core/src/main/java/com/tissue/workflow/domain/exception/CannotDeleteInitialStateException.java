package com.tissue.workflow.domain.exception;

import static com.tissue.exception.ErrorContextKeys.STATE;
import static com.tissue.exception.ErrorContextKeys.WORKFLOW;
import static com.tissue.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.exception.base.BadRequestException;

public class CannotDeleteInitialStateException extends BadRequestException {

    public CannotDeleteInitialStateException(Long workflowId, String workflowName, String stateName) {
        super(WorkflowErrorCode.CANNOT_DELETE_INITIAL_STATE);
        addContext(WORKFLOW_ID, workflowId);
        addContext(WORKFLOW, workflowName);
        addContext(STATE, stateName);
    }
}
