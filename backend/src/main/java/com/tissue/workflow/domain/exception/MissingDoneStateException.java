package com.tissue.workflow.domain.exception;

import com.tissue.common.exception.base.BadRequestException;

public class MissingDoneStateException extends BadRequestException {

	public MissingDoneStateException() {
		super("Workflow must have at least one 'DONE' state.");
	}
}
