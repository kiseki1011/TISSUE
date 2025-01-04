package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class ReviewerAlreadyExistsException extends IssueException {

	private static final String MESSAGE = "Already assigned as a reviewer.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public ReviewerAlreadyExistsException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public ReviewerAlreadyExistsException(String message) {
		super(message, HTTP_STATUS);
	}
}
