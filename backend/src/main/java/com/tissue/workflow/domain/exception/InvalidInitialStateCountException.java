package com.tissue.workflow.domain.exception;

import com.tissue.global.exception.base.BadRequestException;

public class InvalidInitialStateCountException extends BadRequestException {

    public InvalidInitialStateCountException(int foundCount) {
        super(WorkflowErrorCode.INVALID_INITIAL_STATE_COUNT);
        addContext("foundCount", foundCount);
    }
}
