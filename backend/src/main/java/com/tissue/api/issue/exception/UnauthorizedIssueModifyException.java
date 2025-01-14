package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class UnauthorizedIssueModifyException extends IssueException {

	private static final String MESSAGE = "You do not have authorization to modify this Issue.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.UNAUTHORIZED;

	public UnauthorizedIssueModifyException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public UnauthorizedIssueModifyException(String message) {
		super(message, HTTP_STATUS);
	}
}
