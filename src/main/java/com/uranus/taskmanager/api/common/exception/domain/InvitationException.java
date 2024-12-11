package com.uranus.taskmanager.api.common.exception.domain;

import org.springframework.http.HttpStatus;

import com.uranus.taskmanager.api.common.exception.TissueException;

public class InvitationException extends TissueException {

	public InvitationException(String message, HttpStatus httpStatus) {
		super(message, httpStatus);
	}

	public InvitationException(String message, HttpStatus httpStatus, Throwable cause) {
		super(message, httpStatus, cause);
	}
}
