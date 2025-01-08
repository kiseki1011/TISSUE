package com.tissue.api.position.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.PositionException;

public class DuplicatePositionAssignmentException extends PositionException {

	private static final String MESSAGE = "Position is already assigned to this workspace member.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public DuplicatePositionAssignmentException(String message) {
		super(message, HTTP_STATUS);
	}

	public DuplicatePositionAssignmentException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
