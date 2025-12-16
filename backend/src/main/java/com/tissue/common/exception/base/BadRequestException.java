package com.tissue.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.common.exception.TissueException;

public abstract class BadRequestException extends TissueException {

	public BadRequestException(String message) {
		super(message);
	}

	protected BadRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.BAD_REQUEST;
	}
}
