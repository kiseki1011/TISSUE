package com.tissue.workflow.application.dto.response;

import com.tissue.common.enums.ColorType;
import com.tissue.workflow.domain.Workflow;
import org.jspecify.annotations.Nullable;

public record WorkflowSummary(
        Long id,
        String name,
        @Nullable String description,
        ColorType color,
        boolean isSystemProvided,
        boolean isArchived) {

    public static WorkflowSummary from(Workflow wf) {
        return new WorkflowSummary(
                wf.getId(),
                wf.getName().toString(),
                wf.getDescription(),
                wf.getColor(),
                wf.isSystemProvided(),
                wf.isArchived());
    }
}
