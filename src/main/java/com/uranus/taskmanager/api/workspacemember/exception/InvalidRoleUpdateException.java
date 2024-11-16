package com.uranus.taskmanager.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.WorkspaceMemberException;

public class InvalidRoleUpdateException extends WorkspaceMemberException {
	private static final String MESSAGE = "Invalid role selection for update";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public InvalidRoleUpdateException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidRoleUpdateException(String message) {
		super(message, HTTP_STATUS);
	}

	public InvalidRoleUpdateException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
