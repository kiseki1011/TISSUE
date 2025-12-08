package com.tissue.api.workflow.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class StateNotFoundException extends ResourceNotFoundException {

	public StateNotFoundException(Long stateId, Long workflowId) {
		super("Workflow state not found with state id '%d' and workflow id '%d."
			.formatted(stateId, workflowId));
		addContext("stateId", stateId);
		addContext("workflowId", workflowId);
	}
}
