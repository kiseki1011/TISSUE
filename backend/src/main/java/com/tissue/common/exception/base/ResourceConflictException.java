package com.tissue.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.common.exception.ErrorCode;
import com.tissue.common.exception.TissueException;

public class ResourceConflictException extends TissueException {

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.CONFLICT;
	}

	public ResourceConflictException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ResourceConflictException(ErrorCode errorCode, String debugMessage) {
		super(errorCode, debugMessage);
	}
}
