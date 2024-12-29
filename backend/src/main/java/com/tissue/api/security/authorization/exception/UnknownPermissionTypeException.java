package com.tissue.api.security.authorization.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.AuthorizationException;

public class UnknownPermissionTypeException extends AuthorizationException {

	private static final String MESSAGE = "Permission type does not exist.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.FORBIDDEN;

	public UnknownPermissionTypeException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public UnknownPermissionTypeException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
