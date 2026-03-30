package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.workflow.domain.WorkflowTransition;

public record TransitionDetail(Long workflowId, Long transitionId, String displayLabel) {
    public static TransitionDetail from(WorkflowTransition transition) {
        return new TransitionDetail(transition.getWorkflow().getId(), transition.getId(), transition.getDisplayName());
    }
}
