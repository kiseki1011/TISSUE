package com.tissue.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.common.exception.ErrorCode;
import com.tissue.common.exception.TissueException;

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
