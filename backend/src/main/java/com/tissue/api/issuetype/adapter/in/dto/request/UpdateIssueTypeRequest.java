package com.tissue.api.issuetype.adapter.in.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issuetype.application.dto.request.PatchIssueTypeCommand;

import jakarta.validation.constraints.Size;

public record UpdateIssueTypeRequest(
	JsonNullable<@Size(max = 255) String> description,
	JsonNullable<ColorType> color
) {
	public PatchIssueTypeCommand toCommand(String workspaceKey, String projectKey, Long id) {
		return PatchIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.id(id)
			.description(description)
			.color(color)
			.build();
	}
}
