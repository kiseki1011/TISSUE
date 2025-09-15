package com.tissue.api.issue.base.application.dto;

import lombok.Builder;

@Builder
public record RenameIssueTypeCommand(
	String workspaceKey,
	Long id,
	String label
) {
}
