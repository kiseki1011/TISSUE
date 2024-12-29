package com.tissue.api.security.authorization.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.AuthorizationException;

public class PermissionMismatchException extends AuthorizationException {

	private static final String MESSAGE = "Permission type of request does not match with current permission type.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.FORBIDDEN;

	public PermissionMismatchException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public PermissionMismatchException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
