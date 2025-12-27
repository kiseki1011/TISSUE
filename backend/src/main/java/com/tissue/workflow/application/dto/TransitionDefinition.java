package com.tissue.workflow.application.dto;

import com.tissue.common.vo.Name;

import lombok.Builder;

@Builder
public record TransitionDefinition(
	EntityRef transitionRef,
	Name name,
	String description,
	EntityRef sourceStateRef,
	EntityRef targetStateRef
) {
}
