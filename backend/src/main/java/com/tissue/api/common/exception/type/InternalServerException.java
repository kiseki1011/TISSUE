package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public abstract class InternalServerException extends TissueException {

	public InternalServerException(String message) {
		super(message);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}
}
