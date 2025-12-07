package com.tissue.api.workflow.application.dto;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.domain.enums.StateCategory;

public record StateDefinition(
	EntityRef stateRef,
	Label label,
	String description,
	ColorType color,
	StateCategory category
) {
}
