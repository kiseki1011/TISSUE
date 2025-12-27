package com.tissue.global.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.global.exception.ErrorCode;
import com.tissue.global.exception.TissueException;

public class BadRequestException extends TissueException {

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.BAD_REQUEST;
	}

	public BadRequestException(ErrorCode errorCode) {
		super(errorCode);
	}

	public BadRequestException(ErrorCode errorCode, String loggingMessage) {
		super(errorCode, loggingMessage);
	}
}
