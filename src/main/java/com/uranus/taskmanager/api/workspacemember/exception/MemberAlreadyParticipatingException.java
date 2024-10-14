package com.uranus.taskmanager.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

public class MemberAlreadyParticipatingException extends WorkspaceMemberException {
	private static final String MESSAGE = "Current login member is already participating in this Workspace";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public MemberAlreadyParticipatingException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public MemberAlreadyParticipatingException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
