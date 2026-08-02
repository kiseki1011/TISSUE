package com.tissue.feature.workflow.application.dto.response;

import com.tissue.feature.workflow.domain.VcsAutomationSettings;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.shared.enums.ColorType;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record WorkflowDetail(
        Long id,
        String name,
        @Nullable String description,
        ColorType color,
        boolean isSystemProvided,
        Long version,
        Long initialStateId,
        @Nullable Long vcsPrOpenedTransitionId,
        @Nullable Long vcsPrMergedTransitionId,
        List<StateDetail> states,
        List<TransitionDetail> transitions) {

    public static WorkflowDetail from(Workflow wf) {
        VcsAutomationSettings vcs = wf.getVcsSettings();
        return new WorkflowDetail(
                wf.getId(),
                wf.getName(),
                wf.getDescription(),
                wf.getColor(),
                wf.isSystemProvided(),
                wf.getVersion(),
                wf.getInitialState().getId(),
                transitionId(vcs == null ? null : vcs.getVcsPrOpenedTransition()),
                transitionId(vcs == null ? null : vcs.getVcsPrMergedTransition()),
                wf.getActiveStates().stream().map(StateDetail::from).toList(),
                wf.getTransitions().stream().map(TransitionDetail::from).toList());
    }

    private static @Nullable Long transitionId(@Nullable WorkflowTransition transition) {
        return transition == null ? null : transition.getId();
    }
}
