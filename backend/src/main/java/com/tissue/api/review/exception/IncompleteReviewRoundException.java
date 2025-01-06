package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class IncompleteReviewRoundException extends IssueException {

	private static final String MESSAGE = "There are reviewers that have not completed their review for this round.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public IncompleteReviewRoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public IncompleteReviewRoundException(String message) {
		super(message, HTTP_STATUS);
	}
}
