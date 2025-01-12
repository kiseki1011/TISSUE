package com.tissue.api.review.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class UnauthorizedReviewerModificationException extends IssueException {

	private static final String MESSAGE = "Only the reviewer themselves or issue assignees can remove reviewers.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public UnauthorizedReviewerModificationException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public UnauthorizedReviewerModificationException(String message) {
		super(message, HTTP_STATUS);
	}
}
