package com.tissue.feature.project.application.dto.request;

import com.tissue.feature.project.domain.ProjectVisibility;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record UpdateProjectCommand(
        JsonNullable<String> title,
        JsonNullable<String> description,
        JsonNullable<ProjectVisibility> projectVisibility) {}
