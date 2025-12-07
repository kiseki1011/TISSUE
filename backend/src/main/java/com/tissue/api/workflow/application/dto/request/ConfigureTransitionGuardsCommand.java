package com.tissue.api.workflow.application.dto.request;

import java.util.List;

import com.tissue.api.workflow.application.dto.GuardConfigData;

import lombok.Builder;

@Builder
public record ConfigureTransitionGuardsCommand(
	String workspaceKey,
	String projectKey,
	Long workflowId,
	Long transitionId,
	List<GuardConfigData> guards
) {
}
