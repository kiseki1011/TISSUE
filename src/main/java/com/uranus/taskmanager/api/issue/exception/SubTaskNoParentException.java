package com.uranus.taskmanager.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.IssueException;

public class SubTaskNoParentException extends IssueException {

	private static final String MESSAGE = "Sub-task type issues must have a parent issue.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public SubTaskNoParentException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public SubTaskNoParentException(String message) {
		super(message, HTTP_STATUS);
	}
}
