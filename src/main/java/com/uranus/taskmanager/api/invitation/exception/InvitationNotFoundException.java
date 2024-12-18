package com.uranus.taskmanager.api.invitation.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.InvitationException;

public class InvitationNotFoundException extends InvitationException {
	private static final String MESSAGE = "Invitation was not found for the given code";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public InvitationNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}
}
