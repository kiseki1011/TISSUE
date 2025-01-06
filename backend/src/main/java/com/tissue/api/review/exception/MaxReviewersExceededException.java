package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class MaxReviewersExceededException extends IssueException {

	private static final String MESSAGE = "The number of reviewers for this issue exceeded the limit.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public MaxReviewersExceededException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public MaxReviewersExceededException(String message) {
		super(message, HTTP_STATUS);
	}
}
