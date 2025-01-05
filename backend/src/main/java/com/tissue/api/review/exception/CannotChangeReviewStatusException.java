package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class CannotChangeReviewStatusException extends IssueException {

	private static final String MESSAGE = "Can only update review status if currently PENDING status.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public CannotChangeReviewStatusException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public CannotChangeReviewStatusException(String message) {
		super(message, HTTP_STATUS);
	}
}
