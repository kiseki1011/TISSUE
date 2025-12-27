package com.tissue.global.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.global.exception.ErrorCode;
import com.tissue.global.exception.TissueException;

public class RequestTimeoutException extends TissueException {

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.REQUEST_TIMEOUT;
	}

	public RequestTimeoutException(ErrorCode errorCode) {
		super(errorCode);
	}

	public RequestTimeoutException(ErrorCode errorCode, String debugMessage) {
		super(errorCode, debugMessage);
	}

	public RequestTimeoutException(ErrorCode errorCode, Throwable cause) {
		super(errorCode, cause);
	}
}
