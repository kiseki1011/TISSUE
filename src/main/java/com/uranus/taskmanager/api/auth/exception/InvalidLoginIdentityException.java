package com.uranus.taskmanager.api.auth.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.auth.exception.AuthenticationException;

public class InvalidLoginIdentityException extends AuthenticationException {

	private static final String MESSAGE = "Please provide a valid login ID or Email.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public InvalidLoginIdentityException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidLoginIdentityException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
