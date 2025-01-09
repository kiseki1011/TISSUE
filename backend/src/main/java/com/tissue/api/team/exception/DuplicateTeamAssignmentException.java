package com.tissue.api.team.exception;

import org.springframework.http.HttpStatus;

import com.tissue.api.common.exception.domain.PositionException;

public class DuplicateTeamAssignmentException extends PositionException {

	private static final String MESSAGE = "Team is already assigned to this workspace member.";
	private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

	public DuplicateTeamAssignmentException(String message) {
		super(message, HTTP_STATUS);
	}

	public DuplicateTeamAssignmentException(Throwable cause) {
		super(MESSAGE, HTTP_STATUS, cause);
	}
}
