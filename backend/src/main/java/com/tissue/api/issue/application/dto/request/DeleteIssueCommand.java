package com.tissue.api.issue.application.dto.request;

public record DeleteIssueCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long memberId
) {
}
