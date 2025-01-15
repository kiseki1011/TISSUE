package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class DuplicateIssueRelationException extends IssueException {

	private static final String MESSAGE = "Same issue relation already exists.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public DuplicateIssueRelationException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public DuplicateIssueRelationException(String message) {
		super(message, HTTP_STATUS);
	}
}
