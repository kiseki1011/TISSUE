package com.tissue.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.IssueException;

public class CannotDeleteParentOfSubTaskException extends IssueException {

	private static final String MESSAGE = "Cannot delete a Sub-tasks's parent issue.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public CannotDeleteParentOfSubTaskException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public CannotDeleteParentOfSubTaskException(String message) {
		super(message, HTTP_STATUS);
	}
}
