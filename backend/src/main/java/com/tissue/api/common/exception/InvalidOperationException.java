package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOperationException extends TissueException {
	public InvalidOperationException(String message) {
		super(message, HttpStatus.BAD_REQUEST);
	}
}
