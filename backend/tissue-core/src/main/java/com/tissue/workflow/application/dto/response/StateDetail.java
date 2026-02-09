package com.tissue.workflow.application.dto.response;

import com.tissue.enums.ColorType;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.enums.StateCategory;
import org.jspecify.annotations.Nullable;

public record StateDetail(
        Long id,
        String label,
        @Nullable String description,
        ColorType color,
        StateCategory category,
        long activeIssueCount) {

    public static StateDetail of(WorkflowState s, long count) {
        return new StateDetail(s.getId(), s.getDisplayName(), s.getDescription(), s.getColor(), s.getCategory(), count);
    }
}
