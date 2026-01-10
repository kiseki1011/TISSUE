package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.STATE;
import static com.tissue.global.exception.ContextKeys.WORKFLOW;
import static com.tissue.global.exception.ContextKeys.WORKFLOW_ID;

import com.tissue.global.exception.base.BadRequestException;

public class CannotDeleteInitialStateException extends BadRequestException {

    public CannotDeleteInitialStateException(Long workflowId, String workflowName, String stateName) {
        super(WorkflowErrorCode.CANNOT_DELETE_INITIAL_STATE);
        addContext(WORKFLOW_ID, workflowId);
        addContext(WORKFLOW, workflowName);
        addContext(STATE, stateName);
    }
}
