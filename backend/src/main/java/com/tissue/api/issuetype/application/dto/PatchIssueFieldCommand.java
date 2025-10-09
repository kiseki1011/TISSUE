package com.tissue.api.issuetype.application.dto;

import org.openapitools.jackson.nullable.JsonNullable;

import lombok.Builder;

@Builder
public record PatchIssueFieldCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId,
	JsonNullable<String> description,
	JsonNullable<Boolean> required
) {
}
