package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class ReviewerNotFoundException extends IssueException {

	private static final String MESSAGE = "Reviewer was not found.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public ReviewerNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public ReviewerNotFoundException(String message) {
		super(message, HTTP_STATUS);
	}
}
