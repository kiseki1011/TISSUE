package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class TissueException extends RuntimeException {

	private final HttpStatus httpStatus;

	protected TissueException(String message, HttpStatus httpStatus) {
		super(message);
		this.httpStatus = httpStatus;
	}

	protected TissueException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, cause);
		this.httpStatus = httpStatus;
	}
}
