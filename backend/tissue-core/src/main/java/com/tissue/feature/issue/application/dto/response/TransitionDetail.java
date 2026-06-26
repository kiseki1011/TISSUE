package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.application.dto.response.info.StateInfo;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.guard.GuardViolation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
        name = "AvailableTransition",
        description = "A workflow transition available from the issue's current state, "
                + "with guard evaluation results so the client can pre-render disabled buttons.")
public record TransitionDetail(
        Long workflowId,
        Long transitionId,
        String displayLabel,
        StateInfo targetState,
        boolean canExecute,
        List<GuardViolation> blockedReasons) {

    public static TransitionDetail from(WorkflowTransition transition, List<GuardViolation> blockedReasons) {
        return new TransitionDetail(
                transition.getWorkflow().getId(),
                transition.getId(),
                transition.getDisplayName(),
                StateInfo.from(transition.getTargetState()),
                blockedReasons.isEmpty(),
                blockedReasons);
    }
}
