package com.tissue.api.workflow.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class InvalidTodoStateCountException extends BadRequestException {

	public InvalidTodoStateCountException(int foundCount) {
		super("Workflow must have exactly one 'TODO' state. Found %d 'TODO' states.".formatted(foundCount));
		addContext("foundCount", foundCount);
	}
}
