package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class DuplicateResourceException extends TissueException {

	public DuplicateResourceException(String message) {
		super(message, HttpStatus.CONFLICT);
	}

	public DuplicateResourceException(String message, Throwable cause) {
		super(message, HttpStatus.CONFLICT, cause);
	}
}
