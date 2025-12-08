package com.tissue.api.workflow.application.dto;

import com.tissue.api.common.vo.Label;

import lombok.Builder;

@Builder
public record TransitionDefinition(
	EntityRef transitionRef,
	Label label,
	String description,
	EntityRef sourceStateRef,
	EntityRef targetStateRef
) {
}
