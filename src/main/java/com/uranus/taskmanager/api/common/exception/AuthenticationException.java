package com.uranus.taskmanager.api.common.exception;

import org.springframework.http.HttpStatus;

public abstract class AuthenticationException extends CommonException {

	public AuthenticationException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public AuthenticationException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
