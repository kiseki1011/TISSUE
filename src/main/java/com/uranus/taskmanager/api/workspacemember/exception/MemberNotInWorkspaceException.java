package com.uranus.taskmanager.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.WorkspaceMemberException;

public class MemberNotInWorkspaceException extends WorkspaceMemberException {
	private static final String MESSAGE = "Member was not found in this workspace";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public MemberNotInWorkspaceException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public MemberNotInWorkspaceException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
