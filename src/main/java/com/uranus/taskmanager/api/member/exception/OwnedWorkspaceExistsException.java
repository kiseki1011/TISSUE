package com.uranus.taskmanager.api.member.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.MemberException;

public class OwnedWorkspaceExistsException extends MemberException {
	private static final String MESSAGE = "You currently have one or more owned workspaces.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public OwnedWorkspaceExistsException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public OwnedWorkspaceExistsException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
