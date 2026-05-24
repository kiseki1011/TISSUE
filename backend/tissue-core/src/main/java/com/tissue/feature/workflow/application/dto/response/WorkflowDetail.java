package com.tissue.feature.workflow.application.dto.response;

import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.shared.enums.ColorType;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record WorkflowDetail(
        Long id,
        String name,
        @Nullable String description,
        ColorType color,
        boolean isSystemProvided,
        Long initialStateId,
        List<StateDetail> states,
        List<TransitionDetail> transitions) {

    public static WorkflowDetail from(Workflow wf) {
        return new WorkflowDetail(
                wf.getId(),
                wf.getName(),
                wf.getDescription(),
                wf.getColor(),
                wf.isSystemProvided(),
                wf.getInitialState().getId(),
                wf.getActiveStates().stream().map(StateDetail::from).toList(),
                wf.getTransitions().stream().map(TransitionDetail::from).toList());
    }
}
