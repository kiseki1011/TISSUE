package com.uranus.taskmanager.api.invitation.exception;

import org.springframework.http.HttpStatus;

public class InvalidInvitationStatusException extends InvitationException {
	private static final String MESSAGE = "Invitation has already been processed";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public InvalidInvitationStatusException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidInvitationStatusException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
