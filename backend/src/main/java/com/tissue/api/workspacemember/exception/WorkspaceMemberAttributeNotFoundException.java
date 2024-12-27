package com.tissue.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.WorkspaceMemberException;

public class WorkspaceMemberAttributeNotFoundException extends WorkspaceMemberException {

	private static final String MESSAGE = "Workspace member ID attribute not found in request context.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public WorkspaceMemberAttributeNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public WorkspaceMemberAttributeNotFoundException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
