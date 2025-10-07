package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class InvalidCustomFieldException extends TissueException {

	public InvalidCustomFieldException(String message) {
		super(message, HttpStatus.BAD_REQUEST);
	}

	public InvalidCustomFieldException(String message, Throwable cause) {
		super(message, HttpStatus.BAD_REQUEST, cause);
	}
}
