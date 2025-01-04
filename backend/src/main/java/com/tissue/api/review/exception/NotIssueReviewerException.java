package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class NotIssueReviewerException extends IssueException {

	private static final String MESSAGE = "Must be added as a reviewer for this issue to create a review.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public NotIssueReviewerException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public NotIssueReviewerException(String message) {
		super(message, HTTP_STATUS);
	}
}
