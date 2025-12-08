package com.tissue.api.workflow.domain.exception;

import com.tissue.api.common.exception.base.ResourceNotFoundException;

public class TransitionNotFoundException extends ResourceNotFoundException {

	public TransitionNotFoundException(Long transitionId, Long workflowId) {
		super("Workflow transition not found with transition id '%d' and workflow id '%d."
			.formatted(transitionId, workflowId));
		addContext("transitionId", transitionId);
		addContext("workflowId", workflowId);
	}
}
