package com.uranus.taskmanager.api.issue.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.domain.IssueException;

public class SubTaskWrongParentTypeException extends IssueException {

	private static final String MESSAGE = "A Sub-task must have a parent "
		+ "and can only have STORY, TASK, or BUG type as the parent issue.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public SubTaskWrongParentTypeException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public SubTaskWrongParentTypeException(String message) {
		super(message, HTTP_STATUS);
	}
}
