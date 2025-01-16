package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class CircularDependencyException extends IssueException {

	private static final String MESSAGE = "Circular dependency was detected.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public CircularDependencyException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public CircularDependencyException(String message) {
		super(message, HTTP_STATUS);
	}
}
