package com.tissue.api.common.exception.type;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.TissueException;

public class AuthenticationFailedException extends TissueException {

	public AuthenticationFailedException(String message) {
		super(message, HttpStatus.UNAUTHORIZED);
	}

	public AuthenticationFailedException(String message, Throwable cause) {
		super(message, HttpStatus.UNAUTHORIZED, cause);
	}
}
