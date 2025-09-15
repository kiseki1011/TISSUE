package com.tissue.api.issue.base.presentation.dto.request;

import com.tissue.api.issue.base.application.dto.RenameIssueFieldCommand;

import jakarta.validation.constraints.NotBlank;

public record RenameIssueFieldRequest(
	@NotBlank(message = "{valid.notblank}") String label
) {
	public RenameIssueFieldCommand toCommand(String workspaceKey, Long issueTypeId, Long issueFieldId) {
		return RenameIssueFieldCommand.builder()
			.workspaceKey(workspaceKey)
			.issueTypeId(issueTypeId)
			.issueFieldId(issueFieldId)
			.label(label)
			.build();
	}
}
