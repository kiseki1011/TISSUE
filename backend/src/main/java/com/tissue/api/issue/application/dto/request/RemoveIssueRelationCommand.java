package com.tissue.api.issue.application.dto.request;

public record RemoveIssueRelationCommand(
	String workspaceKey,
	String sourceIssueKey,
	String targetIssueKey
) {
}
