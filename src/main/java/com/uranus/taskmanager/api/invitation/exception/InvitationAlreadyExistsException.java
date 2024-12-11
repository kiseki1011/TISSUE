package com.uranus.taskmanager.api.invitation.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.InvitationException;

public class InvitationAlreadyExistsException extends InvitationException {
	private static final String MESSAGE = "An invitation for this member is already pending";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public InvitationAlreadyExistsException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvitationAlreadyExistsException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
