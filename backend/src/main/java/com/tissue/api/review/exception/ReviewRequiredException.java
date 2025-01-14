package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class ReviewRequiredException extends IssueException {

	private static final String MESSAGE = "Review is required.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public ReviewRequiredException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public ReviewRequiredException(String message) {
		super(message, HTTP_STATUS);
	}
}
