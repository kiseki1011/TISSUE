package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class IssueTypeMismatchException extends IssueException {

	private static final String MESSAGE = "Issue type does not match the needed type.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public IssueTypeMismatchException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public IssueTypeMismatchException(String message) {
		super(message, HTTP_STATUS);
	}
}
