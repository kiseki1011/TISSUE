package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class PendingReviewExistsException extends IssueException {

	private static final String MESSAGE = "All reviews must be approved.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public PendingReviewExistsException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public PendingReviewExistsException(String message) {
		super(message, HTTP_STATUS);
	}
}
