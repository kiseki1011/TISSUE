package com.tissue.api.workflow.application.dto.request;

import java.util.List;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;
import com.tissue.api.workflow.application.dto.StateDefinition;
import com.tissue.api.workflow.application.dto.TransitionDefinition;

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
