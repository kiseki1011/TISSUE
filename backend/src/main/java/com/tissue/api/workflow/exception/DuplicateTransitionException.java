package com.tissue.api.workflow.exception;

import com.tissue.api.common.exception.base.ResourceConflictException;

public class DuplicateTransitionException extends ResourceConflictException {

	public DuplicateTransitionException(
		String transitionName,
		String sourceStateName,
		String workflowName,
		Long workflowId
	) {
		super("A transition named '%s' with source state '%s' already exists in workflow '%s'"
			.formatted(transitionName, sourceStateName, workflowName));
		addContext("transitionName", transitionName);
		addContext("workflowName", workflowName);
		addContext("workflowId", workflowId);
	}
}
