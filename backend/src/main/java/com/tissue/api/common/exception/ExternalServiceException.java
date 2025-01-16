package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends TissueException {
	public ExternalServiceException(String message) {
		super(message, HttpStatus.BAD_GATEWAY);
	}
}
