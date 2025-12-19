package com.tissue.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.common.exception.ErrorCode;
import com.tissue.common.exception.TissueException;

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
