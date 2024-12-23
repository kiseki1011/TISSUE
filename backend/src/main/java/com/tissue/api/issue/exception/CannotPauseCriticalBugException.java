package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class CannotPauseCriticalBugException extends IssueException {

	private static final String MESSAGE = "Cannot change the status of a Bug to PAUSE with CRITICAL level or higher.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public CannotPauseCriticalBugException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public CannotPauseCriticalBugException(String message) {
		super(message, HTTP_STATUS);
	}
}
