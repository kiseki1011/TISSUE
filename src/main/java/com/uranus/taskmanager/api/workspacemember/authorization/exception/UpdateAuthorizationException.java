package com.uranus.taskmanager.api.workspacemember.authorization.exception;

import org.springframework.http.HttpStatus;

public class UpdateAuthorizationException extends AuthorizationException {
	private static final String MESSAGE = "You do not have authorization for update or has expired";
	private static final HttpStatus HTTP_STATUS = HttpStatus.FORBIDDEN;

	public UpdateAuthorizationException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public UpdateAuthorizationException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
