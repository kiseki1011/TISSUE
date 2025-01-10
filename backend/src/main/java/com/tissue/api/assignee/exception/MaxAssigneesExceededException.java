package com.tissue.api.assignee.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class MaxAssigneesExceededException extends IssueException {

	private static final String MESSAGE = "The number of assignees for this issue exceeded the limit.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public MaxAssigneesExceededException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public MaxAssigneesExceededException(String message) {
		super(message, HTTP_STATUS);
	}
}
