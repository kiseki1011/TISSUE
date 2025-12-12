package com.tissue.api.workflow.application.dto.response;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.enums.StateCategory;

public record StateDetail(
	Long id,
	String label,
	String description,
	ColorType color,
	StateCategory category,
	long activeIssueCount
) {
	public static StateDetail of(WorkflowState s, long count) {
		return new StateDetail(
			s.getId(), s.getDisplayLabel(), s.getDescription(),
			s.getColor(), s.getCategory(), count
		);
	}
}
