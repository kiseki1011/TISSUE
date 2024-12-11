package com.uranus.taskmanager.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.WorkspaceMemberException;

public class AlreadyJoinedWorkspaceException extends WorkspaceMemberException {
	private static final String MESSAGE = "Member already joined this Workspace";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public AlreadyJoinedWorkspaceException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public AlreadyJoinedWorkspaceException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
