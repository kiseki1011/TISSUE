package com.tissue.api.project.application.dto.response;

import com.tissue.api.project.domain.Project;

public record ProjectCommandResult(
	String workspaceKey,
	String projectKey
) {
	public static ProjectCommandResult from(Project project) {
		return new ProjectCommandResult(project.getWorkspaceKey(), project.getKey());
	}
}
