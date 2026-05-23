package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.workflow.domain.WorkflowTransition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
        name = "AvailableTransition",
        description = "A workflow transition available from the issue's current state, "
                + "with guard evaluation results so the client can pre-render disabled buttons.")
public record TransitionDetail(
        Long workflowId, Long transitionId, String displayLabel, boolean canExecute, List<String> blockedReasons) {

    public static TransitionDetail from(WorkflowTransition transition, List<String> blockedReasons) {
        return new TransitionDetail(
                transition.getWorkflow().getId(),
                transition.getId(),
                transition.getDisplayName(),
                blockedReasons.isEmpty(),
                blockedReasons);
    }
}
