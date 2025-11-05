package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class AuthenticationException extends TissueException {

	public AuthenticationException(String message) {
		super(message);
	}

	@Override
	public final HttpStatus getHttpStatus() {
		return HttpStatus.UNAUTHORIZED;
	}
}
