package com.uranus.taskmanager.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.WorkspaceMemberException;

public class WorkspaceCreationLimitExceededException extends WorkspaceMemberException {
	private static final String MESSAGE = "Maximum workspace creation limit reached.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public WorkspaceCreationLimitExceededException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public WorkspaceCreationLimitExceededException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
