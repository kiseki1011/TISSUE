package com.tissue.api.issue.base.application.dto;

import lombok.Builder;

@Builder
public record UpdateIssueFieldCommand(
	String workspaceKey,
	Long issueTypeId,
	Long issueFieldId,
	String label,
	String description,
	Boolean required
) {
}
