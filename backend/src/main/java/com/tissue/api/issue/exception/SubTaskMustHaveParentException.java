package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class SubTaskMustHaveParentException extends IssueException {

	private static final String MESSAGE = "SubTask type issues must have a parent issue.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public SubTaskMustHaveParentException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public SubTaskMustHaveParentException(String message) {
		super(message, HTTP_STATUS);
	}
}
