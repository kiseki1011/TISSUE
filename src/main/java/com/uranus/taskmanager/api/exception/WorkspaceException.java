package com.uranus.taskmanager.api.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.CommonException;

public abstract class WorkspaceException extends CommonException {
	public WorkspaceException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public WorkspaceException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
