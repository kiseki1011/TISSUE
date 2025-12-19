package com.tissue.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.common.exception.ErrorCode;
import com.tissue.common.exception.TissueException;

public class ExternalServiceException extends TissueException {

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.SERVICE_UNAVAILABLE;
	}

	public ExternalServiceException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ExternalServiceException(ErrorCode errorCode, String debugMessage) {
		super(errorCode, debugMessage);
	}

	public ExternalServiceException(ErrorCode errorCode, Throwable cause) {
		super(errorCode, cause);
	}
}
