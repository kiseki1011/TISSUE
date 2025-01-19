package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class InternalServerException extends TissueException {
	public InternalServerException(String message) {
		super(message, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
