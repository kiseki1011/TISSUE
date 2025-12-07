package com.tissue.api.workflow.application.dto.request;

import java.util.List;

import com.tissue.api.workflow.application.dto.StateDefinition;
import com.tissue.api.workflow.application.dto.TransitionDefinition;

public record ReplaceWorkflowGraphCommand(
	String workspaceKey,
	String projectKey,
	Long workflowId,
	Long version,
	List<StateDefinition> stateDefinitions,
	List<TransitionDefinition> transitionDefinitions
) {
}
