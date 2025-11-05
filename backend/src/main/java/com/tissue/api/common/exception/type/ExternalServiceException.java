package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public abstract class ExternalServiceException extends TissueException {

	public ExternalServiceException(String message) {
		super(message);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.SERVICE_UNAVAILABLE;
	}
}
