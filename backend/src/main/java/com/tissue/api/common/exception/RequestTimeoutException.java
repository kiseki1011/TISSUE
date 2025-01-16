package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class RequestTimeoutException extends TissueException {
	public RequestTimeoutException(String message) {
		super(message, HttpStatus.REQUEST_TIMEOUT);
	}
}
