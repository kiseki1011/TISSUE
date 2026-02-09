package com.tissue.workflow.application.dto.response;

import com.tissue.enums.ColorType;
import com.tissue.workflow.domain.Workflow;
import org.jspecify.annotations.Nullable;

public record WorkflowSummary(
        Long id, String name, @Nullable String description, ColorType color, boolean isSystemProvided) {

    public static WorkflowSummary from(Workflow wf) {
        return new WorkflowSummary(wf.getId(), wf.getName(), wf.getDescription(), wf.getColor(), wf.isSystemProvided());
    }
}
