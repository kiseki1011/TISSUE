package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class NoReviewersAddedException extends IssueException {

	private static final String MESSAGE = "At least one reviewer must be assigned to request review.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public NoReviewersAddedException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public NoReviewersAddedException(String message) {
		super(message, HTTP_STATUS);
	}
}
