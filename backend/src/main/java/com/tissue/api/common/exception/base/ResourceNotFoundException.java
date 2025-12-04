package com.tissue.api.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public abstract class ResourceNotFoundException extends TissueException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.NOT_FOUND;
	}
}
