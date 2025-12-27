package com.tissue.workflow.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Name;

import lombok.Builder;

@Builder
public record UpdateWorkflowCommand(
	String workspaceKey,
	String projectKey,
	Long workflowId,
	JsonNullable<Name> name,
	JsonNullable<String> description,
	JsonNullable<ColorType> color
) {
}
