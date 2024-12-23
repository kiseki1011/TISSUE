package com.tissue.api.workspace.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.WorkspaceException;

public class WorkspaceNotFoundException extends WorkspaceException {

	private static final String MESSAGE = "Workspace was not found for the given code";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public WorkspaceNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public WorkspaceNotFoundException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
