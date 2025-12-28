package com.tissue.project.application.dto.response;

import com.tissue.project.domain.Project;

public record ProjectCommandResult(String workspaceKey, String projectKey) {
    public static ProjectCommandResult from(Project project) {
        return new ProjectCommandResult(project.getWorkspaceKey(), project.getKey());
    }
}
