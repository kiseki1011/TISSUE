package com.tissue.workflow.application.dto;

import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Label;
import com.tissue.workflow.domain.enums.StateCategory;

import lombok.Builder;

@Builder
public record StateDefinition(
	EntityRef stateRef,
	Label label,
	String description,
	ColorType color,
	StateCategory category
) {
}
