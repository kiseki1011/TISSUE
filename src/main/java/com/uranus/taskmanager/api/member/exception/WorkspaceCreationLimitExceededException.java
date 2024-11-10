package com.uranus.taskmanager.api.member.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.MemberException;

public class WorkspaceCreationLimitExceededException extends MemberException {
	private static final String MESSAGE = "Maximum workspace creation limit of 50 reached";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public WorkspaceCreationLimitExceededException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public WorkspaceCreationLimitExceededException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
