package com.tissue.api.workflow.application.dto;

import java.util.List;

public record ReplaceWorkflowGraphCommand(
	String workspaceKey,
	String projectKey,
	Long workflowId,
	Long version,
	List<StateDefinition> stateDefinitions,
	List<TransitionDefinition> transitionDefinitions
) {
}
