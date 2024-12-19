package com.uranus.taskmanager.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.IssueException;

public class DirectUpdateToInReviewException extends IssueException {

	private static final String MESSAGE = "Cannot directly change status to IN_REVIEW. "
		+ "Status will be automatically updated when reviews are pending.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public DirectUpdateToInReviewException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public DirectUpdateToInReviewException(String message) {
		super(message, HTTP_STATUS);
	}
}
