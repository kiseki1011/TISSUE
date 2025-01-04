package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class DuplicateReviewInRoundException extends IssueException {

	private static final String MESSAGE = "A reviewer can only write a single review per round.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public DuplicateReviewInRoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public DuplicateReviewInRoundException(String message) {
		super(message, HTTP_STATUS);
	}
}
