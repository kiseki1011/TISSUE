package com.tissue.api.workflow.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class MissingDoneStateException extends BadRequestException {

	public MissingDoneStateException() {
		super("Workflow must have at least one 'DONE' state.");
	}
}
