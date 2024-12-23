package com.uranus.taskmanager.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.IssueException;

public class EpicCannotHaveParentException extends IssueException {

	private static final String MESSAGE = "Epics cannot have a parent issue.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public EpicCannotHaveParentException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public EpicCannotHaveParentException(String message) {
		super(message, HTTP_STATUS);
	}
}
