package com.tissue.api.workflow.application.dto;

import java.util.List;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;

import lombok.Builder;

@Builder
public record CreateWorkflowCommand(
	String workspaceKey,
	String projectKey,
	Label label,
	String description,
	ColorType color,
	List<StateDefinition> stateDefinitions,
	List<TransitionDefinition> transitionDefinitions
) {
}
