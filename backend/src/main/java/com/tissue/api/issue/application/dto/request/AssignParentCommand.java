package com.tissue.api.issue.application.dto.request;

import lombok.Builder;

@Builder
public record AssignParentCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	String parentIssueKey
) {
}
