package com.tissue.workflow.adapter.in.web.dto.request;

import java.util.List;

import com.tissue.workflow.application.dto.GuardConfigData;
import com.tissue.workflow.application.dto.request.ConfigureTransitionGuardsCommand;

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
