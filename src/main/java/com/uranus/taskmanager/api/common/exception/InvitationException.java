package com.uranus.taskmanager.api.common.exception;

import org.springframework.http.HttpStatus;

public class InvitationException extends CommonException {

	public InvitationException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public InvitationException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
