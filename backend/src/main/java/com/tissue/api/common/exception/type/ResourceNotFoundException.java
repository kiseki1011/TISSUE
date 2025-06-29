package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class ResourceNotFoundException extends TissueException {

	public ResourceNotFoundException(String message) {
		super(message, HttpStatus.NOT_FOUND);
	}

	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, HttpStatus.NOT_FOUND, cause);
	}
}
