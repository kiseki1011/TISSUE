package com.tissue.api.security.authentication.exception;

import com.tissue.api.common.exception.type.AuthenticationFailedException;

public class JwtAuthenticationException extends AuthenticationFailedException {

	public JwtAuthenticationException(String message) {
		super(message);
	}

	public JwtAuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}
}
