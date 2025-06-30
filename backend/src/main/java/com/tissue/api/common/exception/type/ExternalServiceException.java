package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class ExternalServiceException extends TissueException {

	public ExternalServiceException(String message) {
		super(message, HttpStatus.SERVICE_UNAVAILABLE);
	}

	public ExternalServiceException(String message, Throwable cause) {
		super(message, HttpStatus.SERVICE_UNAVAILABLE, cause);
	}
}
