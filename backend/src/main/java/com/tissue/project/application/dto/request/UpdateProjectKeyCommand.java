package com.tissue.project.application.dto.request;

public record UpdateProjectKeyCommand(
	String workspaceKey,
	String projectKey,
	String newKey
) {
}
