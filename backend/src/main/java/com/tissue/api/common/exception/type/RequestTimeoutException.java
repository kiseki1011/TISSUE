package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public abstract class RequestTimeoutException extends TissueException {

	protected RequestTimeoutException(String message) {
		super(message);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.REQUEST_TIMEOUT;
	}
}
