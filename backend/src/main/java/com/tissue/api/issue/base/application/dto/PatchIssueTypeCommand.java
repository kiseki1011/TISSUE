package com.tissue.api.issue.base.application.dto;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.enums.ColorType;

import lombok.Builder;

@Builder
public record PatchIssueTypeCommand(
	String workspaceKey,
	Long id,
	JsonNullable<String> description,
	JsonNullable<ColorType> color
) {
}
