package com.uranus.taskmanager.api.workspace.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.WorkspaceException;

public class InvalidMemberCountException extends WorkspaceException {
	private static final String MESSAGE = "The number of members in a workspace cannot go below zero.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public InvalidMemberCountException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidMemberCountException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
