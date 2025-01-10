package com.tissue.api.assignee.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class AssigneeNotFoundException extends IssueException {

	private static final String MESSAGE = "Assginee was not found.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public AssigneeNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public AssigneeNotFoundException(String message) {
		super(message, HTTP_STATUS);
	}
}
