package com.uranus.taskmanager.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidLoginPasswordException extends AuthenticationException {
	private static final String TITLE = "Invalid Login Password";
	private static final String MESSAGE = "Please provide a valid login pasword.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public InvalidLoginPasswordException() {
		super(TITLE, MESSAGE, HTTP_STATUS);
	}

	public InvalidLoginPasswordException(Throwable cause) {
		super(TITLE, MESSAGE, HTTP_STATUS, cause);
	}
}
