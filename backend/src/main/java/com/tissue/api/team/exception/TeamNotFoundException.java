package com.tissue.api.team.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.PositionException;

public class TeamNotFoundException extends PositionException {
	private static final String MESSAGE = "Could not find the particular team for the workspace.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public TeamNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public TeamNotFoundException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
