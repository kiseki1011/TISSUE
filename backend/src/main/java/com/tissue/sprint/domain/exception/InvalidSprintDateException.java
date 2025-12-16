package com.tissue.sprint.domain.exception;

import java.time.Instant;

import com.tissue.common.exception.base.BadRequestException;

public class InvalidSprintDateException extends BadRequestException {

	public InvalidSprintDateException(Instant startDate, Instant endDate) {
		super("Sprint end date(%s) cannot be before start date(%s).".formatted(endDate, startDate));
	}
}
