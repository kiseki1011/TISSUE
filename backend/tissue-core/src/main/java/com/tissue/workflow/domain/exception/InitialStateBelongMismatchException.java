package com.tissue.workflow.domain.exception;

import com.tissue.exception.base.BadRequestException;

public class InitialStateBelongMismatchException extends BadRequestException {

    public InitialStateBelongMismatchException() {
        super(WorkflowErrorCode.INITIAL_STATE_BELONG_MISMATCH);
    }
}
