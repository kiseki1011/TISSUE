package com.tissue.api.workflow.domain.exception;

import com.tissue.api.common.exception.base.ResourceConflictException;
import com.tissue.api.workflow.domain.guard.GuardType;

public class DuplicateGuardTypeException extends ResourceConflictException {

	public DuplicateGuardTypeException(GuardType guardType) {
		super("Duplicate guard type detected: " + guardType);
		addContext("guardType", guardType);
	}
}
