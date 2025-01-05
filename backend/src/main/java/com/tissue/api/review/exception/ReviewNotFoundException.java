package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class ReviewNotFoundException extends IssueException {

	private static final String MESSAGE = "Review was not found.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public ReviewNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public ReviewNotFoundException(String message) {
		super(message, HTTP_STATUS);
	}
}
