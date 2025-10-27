package com.tissue.api.issue.application.dto.response;

import com.tissue.api.workflow.domain.model.WorkflowTransition;

public record TransitionDetail(
	Long workflowId,
	Long transitionId,
	String displayLabel
) {
	public static TransitionDetail from(WorkflowTransition transition) {
		return new TransitionDetail(
			transition.getWorkflow().getId(),
			transition.getId(),
			transition.getDisplayLabel()
		);
	}
}
