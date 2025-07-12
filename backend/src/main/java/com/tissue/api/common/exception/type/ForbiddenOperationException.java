package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class ForbiddenOperationException extends TissueException {

	public ForbiddenOperationException(String message) {
		super(message, HttpStatus.FORBIDDEN);
	}

	public ForbiddenOperationException(String message, Throwable cause) {
		super(message, HttpStatus.FORBIDDEN, cause);
	}
}
