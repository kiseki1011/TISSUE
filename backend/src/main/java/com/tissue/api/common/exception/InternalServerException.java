package com.tissue.api.common.exception;

import org.springframework.http.HttpStatus;

public class InternalServerException extends TissueException {
	public InternalServerException(String message) {
		super(message, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
