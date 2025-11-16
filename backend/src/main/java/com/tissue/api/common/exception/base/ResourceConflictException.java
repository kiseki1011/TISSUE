package com.tissue.api.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public abstract class ResourceConflictException extends TissueException {

	public ResourceConflictException(String message) {
		super(message);
	}

	protected ResourceConflictException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.CONFLICT;
	}
}
