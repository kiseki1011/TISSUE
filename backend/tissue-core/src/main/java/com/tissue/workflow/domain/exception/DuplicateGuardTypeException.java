package com.tissue.workflow.domain.exception;

import static com.tissue.exception.ErrorContextKeys.GUARD_TYPE;

import com.tissue.exception.base.ResourceConflictException;
import com.tissue.workflow.domain.guard.GuardType;

public class DuplicateGuardTypeException extends ResourceConflictException {

    public DuplicateGuardTypeException(GuardType guardType) {
        super(WorkflowErrorCode.DUPLICATE_GUARD_TYPE);
        addContext(GUARD_TYPE, guardType);
    }
}
