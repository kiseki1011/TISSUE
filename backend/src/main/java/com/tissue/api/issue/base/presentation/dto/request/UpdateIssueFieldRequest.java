package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.issue.base.application.dto.UpdateIssueFieldCommand;

import jakarta.validation.constraints.NotBlank;

public record UpdateIssueFieldRequest(
	@NotBlank(message = "{valid.notblank}") String label,
	@NotBlank(message = "{valid.notblank}") String description,
	@NotBlank(message = "{valid.notblank}") Boolean required
) {
	public UpdateIssueFieldCommand toCommand(String workspaceKey, String issueTypeKey, String issueFieldKey) {
		return UpdateIssueFieldCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeKey(issueTypeKey)
			.issueFieldKey(issueFieldKey)
			.label(TextNormalizer.normalizeLabel(label))
			.description(description)
			.required(required)
			.build();
	}
}
