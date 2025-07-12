package com.tissue.api.security.authentication.exception;

import com.tissue.api.common.exception.type.InternalServerException;

public class JwtSecretException extends InternalServerException {

	public JwtSecretException(String message) {
		super(message);
	}

	public JwtSecretException(String message, Throwable cause) {
		super(message, cause);
	}
}
