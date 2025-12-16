package com.tissue.security.authentication.exception;

import com.tissue.common.exception.base.InternalServerException;

public class JwtSecretException extends InternalServerException {

	public JwtSecretException(String message) {
		super(message);
	}

	public JwtSecretException(String message, Throwable cause) {
		super(message, cause);
	}
}
