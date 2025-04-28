package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class InvalidRequestException extends TissueException {
	public InvalidRequestException(String message) {
		super(message, HttpStatus.BAD_REQUEST);
	}
}
