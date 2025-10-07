package com.tissue.api.issue.workflow.application.dto;

import java.util.List;

import com.tissue.api.issue.workflow.application.GuardConfigData;

import lombok.Builder;

@Builder
public record ConfigureTransitionGuardsCommand(
	String workspaceKey,
	Long workflowId,
	Long transitionId,
	List<GuardConfigData> guards
) {
}
