package com.tissue.api.position.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.PositionException;

public class PositionNotAssignedException extends PositionException {

	private static final String MESSAGE = "Position was not assigned to this workspace member.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public PositionNotAssignedException(String message) {
		super(message, HTTP_STATUS);
	}

	public PositionNotAssignedException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
