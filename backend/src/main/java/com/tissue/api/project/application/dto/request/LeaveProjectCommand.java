package com.tissue.api.project.application.dto.request;

public record LeaveProjectCommand(
	String workspaceKey,
	String projectKey,
	Long actorMemberId
) {
}
