package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class ResourceConflictException extends TissueException {
	public ResourceConflictException(String message) {
		super(message, HttpStatus.CONFLICT);
	}
}
