package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.issue.base.application.dto.UpdateIssueFieldCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateIssueFieldRequest(
	@NotBlank(message = "{valid.notblank}") String label,
	String description,
	@NotNull(message = "{valid.notblank}") Boolean required
) {
	public UpdateIssueFieldCommand toCommand(String workspaceKey, Long issueTypeId, Long issueFieldId) {
		return UpdateIssueFieldCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeId(issueTypeId)
			.issueFieldId(issueFieldId)
			.label(TextNormalizer.normalizeLabel(label))
			.description(description)
			.required(required)
			.build();
	}
}
