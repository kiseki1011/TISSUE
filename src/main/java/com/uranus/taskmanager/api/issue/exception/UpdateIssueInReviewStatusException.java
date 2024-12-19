package com.uranus.taskmanager.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.IssueException;

public class UpdateIssueInReviewStatusException extends IssueException {
	private static final String MESSAGE = "Cannot update status while issue is under review.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public UpdateIssueInReviewStatusException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public UpdateIssueInReviewStatusException(String message) {
		super(message, HTTP_STATUS);
	}
}
