package com.tissue.api.assignee.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class DuplicateAssigneeException extends IssueException {

	private static final String MESSAGE = "Already assigned as a assignee.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public DuplicateAssigneeException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public DuplicateAssigneeException(String message) {
		super(message, HTTP_STATUS);
	}
}
