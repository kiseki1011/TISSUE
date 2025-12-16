package com.tissue.workflow.application.dto.request;

import java.util.List;

import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Label;
import com.tissue.workflow.application.dto.StateDefinition;
import com.tissue.workflow.application.dto.TransitionDefinition;

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
