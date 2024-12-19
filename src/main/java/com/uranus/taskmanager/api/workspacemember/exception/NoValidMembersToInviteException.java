package com.uranus.taskmanager.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.WorkspaceMemberException;

public class NoValidMembersToInviteException extends WorkspaceMemberException {
	private static final String MESSAGE = "No avaliable members were found for invitation.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public NoValidMembersToInviteException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public NoValidMembersToInviteException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
