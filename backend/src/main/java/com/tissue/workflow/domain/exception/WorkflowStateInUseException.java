package com.tissue.workflow.domain.exception;

import com.tissue.global.exception.base.BadRequestException;

public class WorkflowStateInUseException extends BadRequestException {

    public WorkflowStateInUseException(String stateNames) {
        super(WorkflowErrorCode.WORKFLOW_STATE_IN_USE);
        addContext("activeStates", stateNames);
    }
}
