package com.uranus.taskmanager.api.workspacemember.exception;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.CommonException;

public abstract class WorkspaceMemberException extends CommonException {

	public WorkspaceMemberException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public WorkspaceMemberException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
