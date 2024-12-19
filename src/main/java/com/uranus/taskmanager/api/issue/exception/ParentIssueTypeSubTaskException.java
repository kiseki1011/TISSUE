package com.uranus.taskmanager.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.IssueException;

public class ParentIssueTypeSubTaskException extends IssueException {

	private static final String MESSAGE = "Parent issue cannot be a SUB_TASK.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public ParentIssueTypeSubTaskException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public ParentIssueTypeSubTaskException(String message) {
		super(message, HTTP_STATUS);
	}
}
