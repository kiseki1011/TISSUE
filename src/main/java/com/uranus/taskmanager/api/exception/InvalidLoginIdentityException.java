package com.uranus.taskmanager.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidLoginIdentityException extends AuthenticationException {

	private static final String TITLE = "Invalid Login ID or Email";
	private static final String MESSAGE = "Please provide a valid login ID or Email.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public InvalidLoginIdentityException() {
		super(TITLE, MESSAGE, HTTP_STATUS);
	}

	public InvalidLoginIdentityException(Throwable cause) {
		super(TITLE, MESSAGE, HTTP_STATUS, cause);
	}
}
