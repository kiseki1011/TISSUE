package com.tissue.api.workflow.application.dto.response;

import com.tissue.api.workflow.domain.WorkflowTransition;

public record TransitionResponse(
	Long workflowId,
	Long transitionId
) {
	public static TransitionResponse from(WorkflowTransition transition) {
		return new TransitionResponse(transition.getWorkflow().getId(), transition.getId());
	}
}
