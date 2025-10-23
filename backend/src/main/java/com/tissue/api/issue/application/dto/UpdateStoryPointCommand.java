package com.tissue.api.issue.application.dto;

public record UpdateStoryPointCommand(
	String workspaceKey,
	String issueKey,
	Integer storyPoint
) {
}
