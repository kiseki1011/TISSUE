package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends TissueException {

	public DuplicateResourceException(String message) {
		super(message, HttpStatus.CONFLICT);
	}

	public DuplicateResourceException(String message, Throwable cause) {
		super(message, HttpStatus.CONFLICT, cause);
	}

	public DuplicateResourceException(String messageCode, Object... args) {
		super(messageCode, args, HttpStatus.CONFLICT);
	}
}
