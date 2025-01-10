package com.tissue.api.assignee.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class InvalidAssigneeException extends IssueException {

	private static final String MESSAGE = "Invalid assignee.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public InvalidAssigneeException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidAssigneeException(String message) {
		super(message, HTTP_STATUS);
	}
}
