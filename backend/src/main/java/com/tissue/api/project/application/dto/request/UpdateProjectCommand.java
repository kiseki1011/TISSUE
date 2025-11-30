package com.tissue.api.project.application.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.project.domain.enums.ProjectRole;
import com.tissue.api.project.domain.enums.ProjectVisibility;

import lombok.Builder;

@Builder
public record UpdateProjectCommand(
	String workspaceKey,
	String projectKey,
	JsonNullable<String> title,
	JsonNullable<String> description,
	JsonNullable<ProjectVisibility> projectVisibility,
	JsonNullable<ProjectRole> defaultJoinRole
) {
}
