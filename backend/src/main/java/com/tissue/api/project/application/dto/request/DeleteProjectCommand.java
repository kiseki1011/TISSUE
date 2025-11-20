package com.tissue.api.project.application.dto.request;

public record DeleteProjectCommand(
	String workspaceKey,
	String projectKey
) {
}
