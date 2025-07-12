package com.tissue.api.security.authentication.exception;

import com.tissue.api.common.exception.type.InternalServerException;

public class JwtCreationException extends InternalServerException {

	public JwtCreationException(String message) {
		super(message);
	}

	public JwtCreationException(String message, Throwable cause) {
		super(message, cause);
	}
}
