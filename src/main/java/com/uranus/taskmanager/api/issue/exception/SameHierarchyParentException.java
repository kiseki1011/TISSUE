package com.uranus.taskmanager.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.IssueException;

public class SameHierarchyParentException extends IssueException {

	private static final String MESSAGE = "The parent issue must not have a same hierarchy as the child issue.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public SameHierarchyParentException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public SameHierarchyParentException(String message) {
		super(message, HTTP_STATUS);
	}
}
