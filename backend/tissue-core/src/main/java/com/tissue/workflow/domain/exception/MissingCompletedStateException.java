package com.tissue.workflow.domain.exception;

import com.tissue.exception.base.BadRequestException;

public class MissingCompletedStateException extends BadRequestException {

    public MissingCompletedStateException() {
        super(WorkflowErrorCode.MISSING_COMPLETED_STATE);
    }
}
