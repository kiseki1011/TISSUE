package com.tissue.api.issue.base.presentation.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.issue.base.application.dto.PatchIssueFieldCommand;

import jakarta.validation.constraints.Size;

public record PatchIssueFieldRequest(
	JsonNullable<@Size(max = 255) String> description,
	JsonNullable<Boolean> required
) {
	public PatchIssueFieldCommand toCommand(String workspaceKey, Long issueTypeId, Long issueFieldId) {
		return PatchIssueFieldCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeId(issueTypeId)
			.issueFieldId(issueFieldId)
			.description(description)
			.required(required)
			.build();
	}
}
