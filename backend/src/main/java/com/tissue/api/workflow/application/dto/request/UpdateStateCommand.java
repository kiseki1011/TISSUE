package com.tissue.api.workflow.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;

import lombok.Builder;

@Builder
public record UpdateStateCommand(
	String workspaceKey,
	String projectKey,
	Long workflowId,
	Long stateId,
	JsonNullable<Label> label,
	JsonNullable<String> description,
	JsonNullable<ColorType> color
) {
}
