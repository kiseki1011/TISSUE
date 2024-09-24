package com.uranus.taskmanager.api.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.CommonException;

public abstract class AuthenticationException extends CommonException {

	public AuthenticationException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public AuthenticationException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
