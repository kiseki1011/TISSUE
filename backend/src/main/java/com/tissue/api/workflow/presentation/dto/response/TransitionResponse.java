package com.tissue.api.workflow.presentation.dto.response;

import com.tissue.api.workflow.domain.model.WorkflowTransition;

public record TransitionResponse(
	Long workflowId,
	Long transitionId
) {
	public static TransitionResponse from(WorkflowTransition transition) {
		return new TransitionResponse(transition.getWorkflow().getId(), transition.getId());
	}
}
