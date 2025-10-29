package com.tissue.api.issue.application.dto.response.info;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.domain.enums.StateCategory;
import com.tissue.api.workflow.domain.model.WorkflowState;

public record StateInfo(
	Long id,
	String displayName,
	StateCategory category,
	ColorType color
	// String icon
) {
	public static StateInfo from(WorkflowState state) {
		return new StateInfo(
			state.getId(),
			state.getDisplayLabel(),
			state.getCategory(),
			state.getColor()
		);
	}
}
