package com.tissue.api.issue.base.application.dto;

import lombok.Builder;

@Builder
public record UpdateIssueFieldCommand(
	String workspaceKey,
	String issueTypeKey,
	String issueFieldKey,
	String label,
	String description,
	Boolean required
) {
}
