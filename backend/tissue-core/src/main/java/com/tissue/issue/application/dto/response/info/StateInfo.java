package com.tissue.issue.application.dto.response.info;

import com.tissue.enums.ColorType;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.enums.StateCategory;

public record StateInfo(Long id, String displayName, StateCategory category, ColorType color
        // String icon
        ) {

    public static StateInfo from(WorkflowState state) {
        return new StateInfo(state.getId(), state.getDisplayName(), state.getCategory(), state.getColor());
    }
}
