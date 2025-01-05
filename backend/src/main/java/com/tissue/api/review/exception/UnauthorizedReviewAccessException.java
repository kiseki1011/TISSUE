package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class UnauthorizedReviewAccessException extends IssueException {

	private static final String MESSAGE = "Does not have permissions to access the review.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public UnauthorizedReviewAccessException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public UnauthorizedReviewAccessException(String message) {
		super(message, HTTP_STATUS);
	}
}
