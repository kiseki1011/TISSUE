package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class CannotRemoveParentException extends IssueException {

	private static final String MESSAGE = "Cannot remove the parent of this Issue.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public CannotRemoveParentException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public CannotRemoveParentException(String message) {
		super(message, HTTP_STATUS);
	}
}
