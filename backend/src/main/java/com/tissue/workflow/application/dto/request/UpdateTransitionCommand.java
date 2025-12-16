package com.tissue.workflow.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.common.vo.Label;

import lombok.Builder;

@Builder
public record UpdateTransitionCommand(
	String workspaceKey,
	String projectKey,
	Long workflowId,
	Long transitionId,
	JsonNullable<Label> label,
	JsonNullable<String> description
) {
}
