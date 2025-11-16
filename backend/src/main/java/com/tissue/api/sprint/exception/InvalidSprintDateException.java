package com.tissue.api.sprint.exception;

import java.time.Instant;

import com.tissue.api.common.exception.base.BadRequestException;

public class InvalidSprintDateException extends BadRequestException {

	public InvalidSprintDateException(Instant startDate, Instant endDate, String sprintKey) {
		super("Sprint end date(%s) cannot be before start date(%s).".formatted(endDate, startDate));
		addContext("startDate", startDate);
		addContext("endDate", endDate);
		addContext("sprintKey", sprintKey);
	}
}
