package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class BlockingIssuesNotCompletedException extends IssueException {

	private static final String MESSAGE = "Blocking issues need to be completed.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public BlockingIssuesNotCompletedException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public BlockingIssuesNotCompletedException(String message) {
		super(message, HTTP_STATUS);
	}
}
