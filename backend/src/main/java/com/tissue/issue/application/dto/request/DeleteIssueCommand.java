package com.tissue.issue.application.dto.request;

public record DeleteIssueCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	Long actorMemberId
) {
}
