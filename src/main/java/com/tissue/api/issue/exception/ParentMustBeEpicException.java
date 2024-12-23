package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class ParentMustBeEpicException extends IssueException {

	private static final String MESSAGE = "Story/Task/Bug type issues can only have Epic as their parent issue.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public ParentMustBeEpicException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public ParentMustBeEpicException(String message) {
		super(message, HTTP_STATUS);
	}
}
