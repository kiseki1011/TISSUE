package com.uranus.taskmanager.api.common.exception;

import org.springframework.http.HttpStatus;

public abstract class PositionException extends CommonException {

	public PositionException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public PositionException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}