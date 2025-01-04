package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class DuplicateReviewerException extends IssueException {

	private static final String MESSAGE = "Already assigned as a reviewer.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public DuplicateReviewerException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public DuplicateReviewerException(String message) {
		super(message, HTTP_STATUS);
	}
}
