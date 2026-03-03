package com.tissue.feature.workflow.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.GUARD_TYPE;
import static com.tissue.shared.exception.ErrorContextKeys.REASON;

import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.exception.base.BadRequestException;

public class InvalidGuardParameterException extends BadRequestException {

    public InvalidGuardParameterException(String reason, GuardType guardType) {
        super(WorkflowErrorCode.INVALID_GUARD_PARAMETER, reason);
        addContext(GUARD_TYPE, guardType);
        addContext(REASON, reason);
    }
}
