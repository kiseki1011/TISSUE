package com.tissue.feature.workflow.application.dto.response;

import com.tissue.feature.issue.application.dto.IssueCountProjection;
import java.util.List;

public record WorkflowStateCounts(Long workflowId, List<StateCount> states) {

    public record StateCount(Long stateId, long activeIssueCount) {}

    public static WorkflowStateCounts from(Long workflowId, List<IssueCountProjection> projections) {
        return new WorkflowStateCounts(
                workflowId,
                projections.stream()
                        .map(p -> new StateCount(p.getStateId(), p.getCount()))
                        .toList());
    }
}
