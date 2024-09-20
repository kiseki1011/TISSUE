package com.uranus.taskmanager.api.exception;

import org.springframework.http.HttpStatus;

public class UserNotLoggedInException extends AuthenticationExcpetion {
	private static final String TITLE = "Login Required";
	private static final String MESSAGE = "Login is required to access.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public UserNotLoggedInException() {
		super(TITLE, MESSAGE, HTTP_STATUS);
	}

	public UserNotLoggedInException(Throwable cause) {
		super(TITLE, MESSAGE, HTTP_STATUS, cause);
	}
}
