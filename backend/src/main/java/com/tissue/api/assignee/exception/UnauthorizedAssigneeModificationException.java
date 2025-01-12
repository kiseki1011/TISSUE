package com.tissue.api.assignee.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class UnauthorizedAssigneeModificationException extends IssueException {

	private static final String MESSAGE = "Only assignees can modify issue assignments.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public UnauthorizedAssigneeModificationException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public UnauthorizedAssigneeModificationException(String message) {
		super(message, HTTP_STATUS);
	}
}
