package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class CannotRemoveReviewerException extends IssueException {

	private static final String MESSAGE = "Cannot remove reviewer.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public CannotRemoveReviewerException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public CannotRemoveReviewerException(String message) {
		super(message, HTTP_STATUS);
	}
}
