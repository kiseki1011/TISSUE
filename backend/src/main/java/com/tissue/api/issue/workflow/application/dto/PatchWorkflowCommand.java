package com.tissue.api.issue.workflow.application.dto;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.base.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record PatchWorkflowCommand(
	String workspaceKey,
	Long id,
	JsonNullable<Label> label,
	JsonNullable<String> description,
	JsonNullable<ColorType> color
) {
}
