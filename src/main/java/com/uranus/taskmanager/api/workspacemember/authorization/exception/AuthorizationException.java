package com.uranus.taskmanager.api.workspacemember.authorization.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.CommonException;

public abstract class AuthorizationException extends CommonException {

	public AuthorizationException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public AuthorizationException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
