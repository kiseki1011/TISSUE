package com.tissue.feature.workflow.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;

public class InvalidInitialStateCountException extends BadRequestException {

    public InvalidInitialStateCountException(int foundCount) {
        super(WorkflowErrorCode.INVALID_INITIAL_STATE_COUNT);
        addContext("foundCount", foundCount);
    }
}
