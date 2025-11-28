package com.tissue.api.issue.application.dto.request;

public record RemoveParentCommand(
	String workspaceKey,
	String projectKey,
	String issueKey
) {
}
