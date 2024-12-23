package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class TissueException extends RuntimeException {
	private final HttpStatus httpStatus;

	public TissueException(String message, HttpStatus httpStatus) {
		super(message);
		this.httpStatus = httpStatus;
	}

	public TissueException(String message,
		HttpStatus httpStatus, Throwable cause) {
		super(message, cause);
		this.httpStatus = httpStatus;
	}
}
