package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.base.application.dto.UpdateIssueTypeCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateIssueTypeRequest(
	@NotBlank(message = "{valid.notblank}") String label,
	@NotBlank(message = "{valid.notblank}") String description,
	@NotNull(message = "{valid.notnull}") ColorType color
) {
	public UpdateIssueTypeCommand toCommand(String workspaceKey, String issueTypeKey) {
		return UpdateIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeKey(issueTypeKey)
			.label(label)
			.description(description)
			.color(color)
			.build();
	}
}
