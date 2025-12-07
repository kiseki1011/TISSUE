package com.tissue.api.workflow.application.dto;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.domain.enums.StateCategory;
import com.tissue.api.workflow.domain.service.EntityRef;

public record StateDefinition(
	EntityRef stateRef,
	Label label,
	String description,
	ColorType color,
	StateCategory category
) {
}
