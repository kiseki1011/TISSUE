package com.tissue.project.application.dto.request;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateProjectCommand(
        String workspaceKey,
        String projectKey,
        JsonNullable<String> title,
        JsonNullable<String> description,
        JsonNullable<ProjectVisibility> projectVisibility,
        JsonNullable<ProjectRole> defaultJoinRole) {}
