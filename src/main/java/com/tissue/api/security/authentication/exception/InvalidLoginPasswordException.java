package com.tissue.api.security.authentication.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.AuthenticationException;

public class InvalidLoginPasswordException extends AuthenticationException {

	private static final String MESSAGE = "The given login password is invalid";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public InvalidLoginPasswordException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidLoginPasswordException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
