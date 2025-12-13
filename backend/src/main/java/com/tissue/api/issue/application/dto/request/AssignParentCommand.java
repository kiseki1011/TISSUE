package com.tissue.api.issue.application.dto.request;

public record AssignParentCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	String parentProjectKey,
	String parentIssueKey,
	Long memberId
) {
}
