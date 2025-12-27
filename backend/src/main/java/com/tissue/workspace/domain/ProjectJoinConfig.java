package com.tissue.workspace.domain;

import com.tissue.project.domain.Project;
import com.tissue.project.domain.enums.ProjectRole;

public record ProjectJoinConfig(Long projectId, String projectKey, ProjectRole role) {
    public static ProjectJoinConfig of(Project project, ProjectRole role) {
        return new ProjectJoinConfig(project.getId(), project.getKey(), role);
    }
}
