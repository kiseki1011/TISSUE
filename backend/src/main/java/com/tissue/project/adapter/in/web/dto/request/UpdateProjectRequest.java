package com.tissue.project.adapter.in.web.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.project.application.dto.request.UpdateProjectCommand;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
	JsonNullable<@Size(min = 2, max = 100) @NotBlank String> title,
	JsonNullable<@Size(max = 255) String> description,
	JsonNullable<ProjectVisibility> projectVisibility,
	JsonNullable<ProjectRole> defaultJoinRole
) {
	public UpdateProjectCommand toCommand(String workspaceKey, String projectKey) {
		return UpdateProjectCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.title(title)
			.description(description)
			.projectVisibility(projectVisibility)
			.defaultJoinRole(defaultJoinRole)
			.build();
	}
}
