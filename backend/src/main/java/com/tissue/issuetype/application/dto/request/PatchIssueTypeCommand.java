package com.tissue.issuetype.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.common.enums.ColorType;

import lombok.Builder;

@Builder
public record PatchIssueTypeCommand(
	String workspaceKey,
	String projectKey,
	Long issueTypeId,
	JsonNullable<String> description,
	JsonNullable<ColorType> color
) {
}
