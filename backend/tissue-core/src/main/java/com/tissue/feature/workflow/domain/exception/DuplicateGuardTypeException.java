package com.tissue.feature.workflow.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.GUARD_TYPE;

import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.exception.base.ResourceConflictException;

public class DuplicateGuardTypeException extends ResourceConflictException {

    public DuplicateGuardTypeException(GuardType guardType) {
        super(WorkflowErrorCode.DUPLICATE_GUARD_TYPE);
        addContext(GUARD_TYPE, guardType);
    }
}
