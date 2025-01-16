package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceConflictException extends TissueException {
	public ResourceConflictException(String message) {
		super(message, HttpStatus.CONFLICT);
	}
}
