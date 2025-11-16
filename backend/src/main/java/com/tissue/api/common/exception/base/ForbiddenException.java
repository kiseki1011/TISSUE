package com.tissue.api.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public abstract class ForbiddenException extends TissueException {

	public ForbiddenException(String message) {
		super(message);
	}

	protected ForbiddenException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.FORBIDDEN;
	}
}
