package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.GUARD_TYPE;
import static com.tissue.global.exception.ContextKeys.REASON;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.workflow.domain.guard.GuardType;

public class InvalidGuardParameterException extends BadRequestException {

    public InvalidGuardParameterException(String reason, GuardType guardType) {
        super(WorkflowErrorCode.INVALID_GUARD_PARAMETER, reason);
        addContext(GUARD_TYPE, guardType);
        addContext(REASON, reason);
    }
}
