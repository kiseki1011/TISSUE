package com.tissue.api.workflow.application.dto;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;

import lombok.Builder;

@Builder
public record UpdateStateCommand(
	String workspaceKey,
	String projectKey,
	Long workflowId,
	Long statusId,
	JsonNullable<Label> label,
	JsonNullable<String> description,
	JsonNullable<ColorType> color
) {
}
