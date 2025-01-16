package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends TissueException {
	public AccessDeniedException(String message) {
		super(message, HttpStatus.FORBIDDEN);
	}
}
