package com.tissue.api.workflow.application.dto;

import com.tissue.api.common.vo.Label;
import com.tissue.api.workflow.domain.service.EntityRef;

public record TransitionDefinition(
	EntityRef transitionRef,
	Label label,
	String description,
	EntityRef sourceStateRef,
	EntityRef targetStateRef
) {
}
