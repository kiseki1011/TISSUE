package com.uranus.taskmanager.api.workspacemember.authorization.exception;

import org.springframework.http.HttpStatus;

public class InvalidWorkspaceCodeInUriException extends AuthorizationException {
	private static final String MESSAGE = "The workspace code in the URI is invalid";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public InvalidWorkspaceCodeInUriException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidWorkspaceCodeInUriException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
