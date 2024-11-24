package com.uranus.taskmanager.api.workspace.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.WorkspaceException;

public class WorkspaceMemberLimitExceededException extends WorkspaceException {
	private static final String MESSAGE = "Maximum number of members that can join this workspace reached.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public WorkspaceMemberLimitExceededException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public WorkspaceMemberLimitExceededException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
