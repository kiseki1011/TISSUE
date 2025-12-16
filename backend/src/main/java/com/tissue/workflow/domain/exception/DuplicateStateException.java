package com.tissue.workflow.domain.exception;

import com.tissue.common.exception.base.ResourceConflictException;

public class DuplicateStateException extends ResourceConflictException {

	public DuplicateStateException(String stateName, String workflowName, Long workflowId) {
		super("A state named '%s' already exists in workflow '%s'.".formatted(stateName, workflowName));
		addContext("stateName", stateName);
		addContext("workflowName", workflowName);
		addContext("workflowId", workflowId);
	}
}
