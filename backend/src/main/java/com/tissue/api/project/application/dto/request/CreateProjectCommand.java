package com.tissue.api.project.application.dto.request;

import lombok.Builder;

@Builder
public record CreateProjectCommand(
	String workspaceKey,
	String projectKey,
	String name,
	String description
) {
}
