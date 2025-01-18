package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends TissueException {
	public ForbiddenOperationException(String message) {
		super(message, HttpStatus.FORBIDDEN);
	}
}
