package com.tissue.api.common.exception.base;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public abstract class AuthenticationException extends TissueException {

	public AuthenticationException(String message) {
		super(message);
	}

	protected AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.UNAUTHORIZED;
	}
}
