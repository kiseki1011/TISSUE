package com.tissue.global.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.global.exception.ErrorCode;
import com.tissue.global.exception.TissueException;

public class InternalServerException extends TissueException {

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	public InternalServerException(ErrorCode errorCode) {
		super(errorCode);
	}

	public InternalServerException(ErrorCode errorCode, String debugMessage) {
		super(errorCode, debugMessage);
	}

	public InternalServerException(ErrorCode errorCode, Throwable cause) {
		super(errorCode, cause);
	}
}
