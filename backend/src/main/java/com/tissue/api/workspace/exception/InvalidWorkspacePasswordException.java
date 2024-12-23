package com.tissue.api.workspace.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.AuthenticationException;

public class InvalidWorkspacePasswordException extends AuthenticationException {
	private static final String MESSAGE = "The given workspace password is invalid";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public InvalidWorkspacePasswordException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidWorkspacePasswordException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
