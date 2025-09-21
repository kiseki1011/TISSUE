package com.tissue.api.issue.base.presentation.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.base.application.dto.PatchIssueTypeCommand;

import jakarta.validation.constraints.Size;

public record PatchIssueTypeRequest(
	JsonNullable<@Size(max = 255) String> description,
	JsonNullable<ColorType> color
) {
	public PatchIssueTypeCommand toCommand(String workspaceKey, Long id) {
		return PatchIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.id(id)
			.description(description)
			.color(color)
			.build();
	}
}
