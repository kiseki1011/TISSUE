package com.uranus.taskmanager.api.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class CommonException extends RuntimeException {
	private final String message;
	private final HttpStatus httpStatus;

	public CommonException(String message, HttpStatus httpStatus) {
		this.message = message;
		this.httpStatus = httpStatus;
	}

	public CommonException(String message,
		HttpStatus httpStatus, Throwable cause) {
		super(cause);
		this.message = message;
		this.httpStatus = httpStatus;
	}
}
