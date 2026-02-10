package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.enums.ColorType;

public record StateInfo(Long id, String displayName, StateCategory category, ColorType color
        // String icon
        ) {

    public static StateInfo from(WorkflowState state) {
        return new StateInfo(state.getId(), state.getDisplayName(), state.getCategory(), state.getColor());
    }
}
