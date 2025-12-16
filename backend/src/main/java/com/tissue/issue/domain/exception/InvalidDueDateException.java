package com.tissue.issue.domain.exception;

import java.time.Instant;

import com.tissue.common.exception.base.BadRequestException;

public class InvalidDueDateException extends BadRequestException {

	public InvalidDueDateException(Instant invalidDueDate, Instant comparedAt) {
		super("Due date cannot be in the past.");
		addContext("invalidDueDate", invalidDueDate);
		addContext("comparedAt", comparedAt);
	}
}
