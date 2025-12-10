package com.tissue.api.issuetype.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.enums.ColorType;

import lombok.Builder;

@Builder
public record PatchIssueTypeCommand(
	String workspaceKey,
	String projectKey,
	Long id,
	JsonNullable<String> description,
	JsonNullable<ColorType> color
) {
}
