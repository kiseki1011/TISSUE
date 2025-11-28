package com.tissue.api.project.application.dto.request;

public record JoinProjectCommand(
	String workspaceKey,
	String projectKey,
	Long actorMemberId
) {
}
