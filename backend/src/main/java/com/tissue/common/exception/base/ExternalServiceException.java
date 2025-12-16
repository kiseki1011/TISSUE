package com.tissue.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.common.exception.TissueException;

public abstract class ExternalServiceException extends TissueException {

	public ExternalServiceException(String message) {
		super(message);
	}

	protected ExternalServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.SERVICE_UNAVAILABLE;
	}
}
