package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class IssueStatusNotChangesRequestedException extends IssueException {

	private static final String MESSAGE = "The issue status must be CHANGES_REQUESTED to start a new review round.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public IssueStatusNotChangesRequestedException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public IssueStatusNotChangesRequestedException(String message) {
		super(message, HTTP_STATUS);
	}
}
