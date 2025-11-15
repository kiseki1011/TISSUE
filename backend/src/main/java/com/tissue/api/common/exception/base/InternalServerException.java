package com.tissue.api.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public abstract class InternalServerException extends TissueException {

	public InternalServerException(String message) {
		super(message);
	}

	protected InternalServerException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}
}
