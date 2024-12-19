package com.uranus.taskmanager.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.IssueException;

public class ParentIssueNotSameWorkspaceException extends IssueException {

	private static final String MESSAGE = "Parent issue must belong to the same workspace of child issue.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public ParentIssueNotSameWorkspaceException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public ParentIssueNotSameWorkspaceException(String message) {
		super(message, HTTP_STATUS);
	}
}
