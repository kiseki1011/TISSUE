package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class IssueStatusNotInReviewException extends IssueException {

	private static final String MESSAGE = "The issue status must be IN_REVIEW to create a review.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public IssueStatusNotInReviewException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public IssueStatusNotInReviewException(String message) {
		super(message, HTTP_STATUS);
	}
}
