package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.project.domain.Project;

public record ProjectResponse(String projectKey) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getKey());
    }
}
