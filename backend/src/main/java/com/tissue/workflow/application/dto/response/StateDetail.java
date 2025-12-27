package com.tissue.workflow.application.dto.response;

import com.tissue.common.enums.ColorType;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.enums.StateCategory;

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
			s.getId(), s.getDisplayName(), s.getDescription(),
			s.getColor(), s.getCategory(), count
		);
	}
}
