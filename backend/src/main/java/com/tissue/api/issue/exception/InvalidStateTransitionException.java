package com.tissue.api.issue.exception;

import com.tissue.api.common.exception.base.BadRequestException;

public class InvalidStateTransitionException extends BadRequestException {

	public InvalidStateTransitionException(
		String issueKey,
		String projectKey,
		String workspaceKey,
		Long transitionId,
		String currentState,
		String requiredSourceState
	) {
		super("Invalid transition for current state");
		addContext("issueKey", issueKey);
		addContext("projectKey", projectKey);
		addContext("workspaceKey", workspaceKey);
		addContext("transitionId", transitionId);
		addContext("currentState", currentState);
		addContext("requiredSourceState", requiredSourceState);
	}
}
