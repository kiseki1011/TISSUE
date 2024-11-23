package com.uranus.taskmanager.api.common.exception;

import org.springframework.http.HttpStatus;

public abstract class AuthorizationException extends CommonException {

	public AuthorizationException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public AuthorizationException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
