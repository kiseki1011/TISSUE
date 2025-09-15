package com.tissue.api.issue.base.presentation.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.base.application.dto.PatchIssueTypeCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatchIssueTypeRequest(
	JsonNullable<@Size(max = 255) String> description,
	JsonNullable<@NotNull ColorType> color
) {
	public PatchIssueTypeCommand toCommand(String workspaceKey, String issueTypeKey) {
		return PatchIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeKey(issueTypeKey)
			.description(description)
			.color(color)
			.build();
	}
}
