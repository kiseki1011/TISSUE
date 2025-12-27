package com.tissue.workflow.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.common.vo.Name;

import lombok.Builder;

@Builder
public record UpdateTransitionCommand(
	String workspaceKey,
	String projectKey,
	Long workflowId,
	Long transitionId,
	JsonNullable<Name> name,
	JsonNullable<String> description
) {
}
