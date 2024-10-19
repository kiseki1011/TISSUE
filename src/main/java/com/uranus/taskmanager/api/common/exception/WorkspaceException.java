package com.uranus.taskmanager.api.common.exception;

import org.springframework.http.HttpStatus;

public abstract class WorkspaceException extends CommonException {
	public WorkspaceException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public WorkspaceException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
