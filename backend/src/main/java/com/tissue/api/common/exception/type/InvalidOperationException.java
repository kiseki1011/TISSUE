package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class InvalidOperationException extends TissueException {

	public InvalidOperationException(String message) {
		super(message, HttpStatus.BAD_REQUEST);
	}

	public InvalidOperationException(String message, Throwable cause) {
		super(message, HttpStatus.BAD_REQUEST, cause);
	}
}
