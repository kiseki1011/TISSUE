package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class InvalidStatusTransitionException extends IssueException {

	private static final String MESSAGE = "This is a invalid status transition.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public InvalidStatusTransitionException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public InvalidStatusTransitionException(String message) {
		super(message, HTTP_STATUS);
	}
}
