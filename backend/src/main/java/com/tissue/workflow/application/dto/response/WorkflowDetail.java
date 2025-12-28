package com.tissue.workflow.application.dto.response;

import com.tissue.common.enums.ColorType;
import com.tissue.issue.application.dto.IssueCountProjection;
import com.tissue.workflow.domain.Workflow;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record WorkflowDetail(
        Long id,
        String name,
        String description,
        ColorType color,
        boolean isSystemProvided,
        boolean isArchived,
        Long initialStateId,
        List<StateDetail> states,
        List<TransitionDetail> transitions) {
    public static WorkflowDetail of(Workflow wf, List<IssueCountProjection> projections) {
        Map<Long, Long> countMap =
                projections.stream()
                        .collect(
                                Collectors.toMap(
                                        IssueCountProjection::stateId,
                                        IssueCountProjection::count));

        return new WorkflowDetail(
                wf.getId(),
                wf.getName().toString(),
                wf.getDescription(),
                wf.getColor(),
                wf.isSystemProvided(),
                wf.isArchived(),
                wf.getInitialState().getId(),
                wf.getActiveStates().stream()
                        .map(s -> StateDetail.of(s, countMap.getOrDefault(s.getId(), 0L)))
                        .toList(),
                wf.getTransitions().stream().map(TransitionDetail::from).toList());
    }
}
