package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class RequestTimeoutException extends TissueException {

	public RequestTimeoutException(String message) {
		super(message, HttpStatus.REQUEST_TIMEOUT);
	}

	public RequestTimeoutException(String message, Throwable cause) {
		super(message, HttpStatus.REQUEST_TIMEOUT, cause);
	}
}
