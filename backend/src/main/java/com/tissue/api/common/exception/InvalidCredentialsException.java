package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends TissueException {

	public InvalidCredentialsException(String message) {
		super(message, HttpStatus.BAD_REQUEST);
	}

	public InvalidCredentialsException(String messageCode, Object... args) {
		super(messageCode, args, HttpStatus.BAD_REQUEST);
	}
}
