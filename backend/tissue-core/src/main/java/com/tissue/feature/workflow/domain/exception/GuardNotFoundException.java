package com.tissue.feature.workflow.domain.exception;

import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.exception.base.InternalServerException;

public class GuardNotFoundException extends InternalServerException {
    public GuardNotFoundException(GuardType type) {
        super(WorkflowErrorCode.GUARD_NOT_FOUND, "GuardType: " + type.name());
    }
}
