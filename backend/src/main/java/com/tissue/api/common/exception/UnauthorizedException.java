package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends TissueException {
	public UnauthorizedException(String message) {
		super(message, HttpStatus.UNAUTHORIZED);
	}
}
