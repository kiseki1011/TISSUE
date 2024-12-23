package com.tissue.api.position.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.PositionException;

public class PositionNotFoundException extends PositionException {
	private static final String MESSAGE = "Could not find the following position for the workspace.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

	public PositionNotFoundException() {
		super(MESSAGE, HTTP_STATUS);
	}

	public PositionNotFoundException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
