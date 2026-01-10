package com.tissue.workflow.domain.exception;

import com.tissue.global.exception.base.BadRequestException;

public class InitialStateBelongMismatchException extends BadRequestException {

    public InitialStateBelongMismatchException() {
        super(WorkflowErrorCode.INITIAL_STATE_BELONG_MISMATCH);
    }
}
