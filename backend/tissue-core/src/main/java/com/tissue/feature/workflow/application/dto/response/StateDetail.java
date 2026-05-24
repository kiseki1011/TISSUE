package com.tissue.feature.workflow.application.dto.response;

import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.enums.ColorType;
import org.jspecify.annotations.Nullable;

public record StateDetail(
        Long id, String label, @Nullable String description, ColorType color, StateCategory category) {

    public static StateDetail from(WorkflowState s) {
        return new StateDetail(s.getId(), s.getDisplayName(), s.getDescription(), s.getColor(), s.getCategory());
    }
}
