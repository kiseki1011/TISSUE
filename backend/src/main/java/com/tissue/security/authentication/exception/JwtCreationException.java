package com.tissue.security.authentication.exception;

import com.tissue.common.exception.base.InternalServerException;

public class JwtCreationException extends InternalServerException {

	public JwtCreationException(String message) {
		super(message);
	}

	public JwtCreationException(String message, Throwable cause) {
		super(message, cause);
	}
}
