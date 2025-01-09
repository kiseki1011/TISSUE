package com.tissue.api.team.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.PositionException;

public class TeamNotAssignedException extends PositionException {

	private static final String MESSAGE = "Team was not assigned to this workspace member.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public TeamNotAssignedException(String message) {
		super(message, HTTP_STATUS);
	}

	public TeamNotAssignedException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
