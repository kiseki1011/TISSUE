package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.GUARD_TYPE;

import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.workflow.domain.guard.GuardType;

public class DuplicateGuardTypeException extends ResourceConflictException {

    public DuplicateGuardTypeException(GuardType guardType) {
        super(WorkflowErrorCode.DUPLICATE_GUARD_TYPE);
        addContext(GUARD_TYPE, guardType);
    }
}
