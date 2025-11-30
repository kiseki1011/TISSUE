package com.tissue.api.issue.application.dto.request;

public record UpdateStoryPointCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	Integer storyPoint
) {
}
