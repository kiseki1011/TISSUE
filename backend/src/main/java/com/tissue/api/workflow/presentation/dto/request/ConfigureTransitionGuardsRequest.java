package com.tissue.api.workflow.presentation.dto.request;

import java.util.List;

import com.tissue.api.workflow.application.GuardConfigData;
import com.tissue.api.workflow.application.dto.ConfigureTransitionGuardsCommand;

public record ConfigureTransitionGuardsRequest(
	List<GuardConfigData> guards
) {
	public ConfigureTransitionGuardsCommand toCommand(String workspaceKey, Long workflowId, Long transitionId) {
		return ConfigureTransitionGuardsCommand.builder()
			.workspaceKey(workspaceKey)
			.workflowId(workflowId)
			.transitionId(transitionId)
			.guards(guards)
			.build();
	}
}
