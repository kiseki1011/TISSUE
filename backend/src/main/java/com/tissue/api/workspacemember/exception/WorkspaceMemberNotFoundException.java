package com.tissue.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.WorkspaceMemberException;

public class WorkspaceMemberNotFoundException extends WorkspaceMemberException {

	private static final String MESSAGE = "WorkspaceMember was not found.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public WorkspaceMemberNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public WorkspaceMemberNotFoundException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
