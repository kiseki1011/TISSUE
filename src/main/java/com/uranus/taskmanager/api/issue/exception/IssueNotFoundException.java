package com.uranus.taskmanager.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.IssueException;

public class IssueNotFoundException extends IssueException {

	private static final String MESSAGE = "Issue was not found.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public IssueNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public IssueNotFoundException(String message) {
		super(message, HTTP_STATUS);
	}
}
