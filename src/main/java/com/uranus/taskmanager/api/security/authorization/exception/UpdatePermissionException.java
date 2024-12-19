package com.uranus.taskmanager.api.security.authorization.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.AuthorizationException;

public class UpdatePermissionException extends AuthorizationException {
	private static final String MESSAGE = "You do not have authorization for update or the authorization has expired";
	private static final HttpStatus HTTP_STATUS = HttpStatus.FORBIDDEN;

	public UpdatePermissionException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public UpdatePermissionException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
