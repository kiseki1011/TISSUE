package com.uranus.taskmanager.api.common.exception.domain;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.TissueException;

public abstract class WorkspaceException extends TissueException {
	public WorkspaceException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public WorkspaceException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
