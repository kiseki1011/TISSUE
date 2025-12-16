package com.tissue.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.common.exception.TissueException;

public abstract class RequestTimeoutException extends TissueException {

	protected RequestTimeoutException(String message) {
		super(message);
	}

	protected RequestTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.REQUEST_TIMEOUT;
	}
}
