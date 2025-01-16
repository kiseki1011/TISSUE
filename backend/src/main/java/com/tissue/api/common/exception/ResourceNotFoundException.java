package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends TissueException {
	public ResourceNotFoundException(String message) {
		super(message, HttpStatus.NOT_FOUND);
	}
}
