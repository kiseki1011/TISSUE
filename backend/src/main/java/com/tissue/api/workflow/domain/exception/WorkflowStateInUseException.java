package com.tissue.api.workflow.domain.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class WorkflowStateInUseException extends BadRequestException {

	public WorkflowStateInUseException(String stateNames) {
		super("Cannot delete workflow states that are currently assigned to active issues: [%s]"
			.formatted(stateNames));
	}
}
