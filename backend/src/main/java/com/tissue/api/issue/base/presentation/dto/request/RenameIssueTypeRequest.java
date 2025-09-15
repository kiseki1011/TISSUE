package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.common.util.TextNormalizer;
import com.tissue.api.issue.base.application.dto.RenameIssueTypeCommand;

import jakarta.validation.constraints.NotBlank;

public record RenameIssueTypeRequest(
	@NotBlank(message = "{valid.notblank}") String label
) {
	public RenameIssueTypeCommand toCommand(String workspaceKey, Long issueTypeId) {
		return RenameIssueTypeCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeKey(issueTypeId)
			.label(TextNormalizer.normalizeLabel(label))
			.build();
	}
}
