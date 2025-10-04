package com.tissue.api.issue.workflow.application.dto;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.issue.base.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record PatchTransitionCommand(
	String workspaceKey,
	Long workflowId,
	Long transitionId,
	JsonNullable<Label> label,
	JsonNullable<String> description
) {
}
