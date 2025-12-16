package com.tissue.project.application.dto.request;

public record DeleteProjectCommand(
	String workspaceKey,
	String projectKey
) {
}
