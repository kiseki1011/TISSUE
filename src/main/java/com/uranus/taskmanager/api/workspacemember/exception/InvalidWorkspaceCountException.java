package com.uranus.taskmanager.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.WorkspaceMemberException;

public class InvalidWorkspaceCountException extends WorkspaceMemberException {
	private static final String MESSAGE = "The count for owned workspaces cannot go below zero.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public InvalidWorkspaceCountException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidWorkspaceCountException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
