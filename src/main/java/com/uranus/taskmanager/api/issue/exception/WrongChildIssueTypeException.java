package com.uranus.taskmanager.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.IssueException;

public class WrongChildIssueTypeException extends IssueException {

	private static final String MESSAGE = "Only EPIC type issues can have non SUB_TASK children.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public WrongChildIssueTypeException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public WrongChildIssueTypeException(String message) {
		super(message, HTTP_STATUS);
	}
}
