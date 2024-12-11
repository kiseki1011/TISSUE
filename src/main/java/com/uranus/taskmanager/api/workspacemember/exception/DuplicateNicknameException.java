package com.uranus.taskmanager.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.WorkspaceMemberException;

public class DuplicateNicknameException extends WorkspaceMemberException {
	private static final String MESSAGE = "Nickname already exists in this workspace.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public DuplicateNicknameException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public DuplicateNicknameException(String message) {
		super(message, HTTP_STATUS);
	}

	public DuplicateNicknameException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
