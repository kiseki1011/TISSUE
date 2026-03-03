package com.tissue.feature.workflow.application.dto.response;

import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.shared.enums.ColorType;
import org.jspecify.annotations.Nullable;

public record WorkflowSummary(
        Long id, String name, @Nullable String description, ColorType color, boolean isSystemProvided) {

    public static WorkflowSummary from(Workflow wf) {
        return new WorkflowSummary(wf.getId(), wf.getName(), wf.getDescription(), wf.getColor(), wf.isSystemProvided());
    }
}
