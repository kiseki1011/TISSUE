package com.tissue.global.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.global.exception.ErrorCode;
import com.tissue.global.exception.TissueException;

public class ResourceConflictException extends TissueException {

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.CONFLICT;
	}

	public ResourceConflictException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ResourceConflictException(ErrorCode errorCode, Throwable e) {
		super(errorCode, e);
	}

	public ResourceConflictException(ErrorCode errorCode, String debugMessage) {
		super(errorCode, debugMessage);
	}
}
