package com.tissue.workflow.domain.exception;

import com.tissue.common.exception.base.ResourceConflictException;
import com.tissue.workflow.domain.guard.GuardType;

public class DuplicateGuardTypeException extends ResourceConflictException {

	public DuplicateGuardTypeException(GuardType guardType) {
		super("Duplicate guard type detected: " + guardType);
		addContext("guardType", guardType);
	}
}
