package com.uranus.taskmanager.api.common.exception;

import org.springframework.http.HttpStatus;

public abstract class WorkspaceMemberException extends CommonException {

	public WorkspaceMemberException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public WorkspaceMemberException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
