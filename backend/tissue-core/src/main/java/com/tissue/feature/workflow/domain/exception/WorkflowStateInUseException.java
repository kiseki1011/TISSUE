package com.tissue.feature.workflow.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;

public class WorkflowStateInUseException extends BadRequestException {

    public WorkflowStateInUseException(String stateNames) {
        super(WorkflowErrorCode.WORKFLOW_STATE_IN_USE);
        addContext("activeStates", stateNames);
    }
}
