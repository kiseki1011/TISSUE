package com.tissue.api.project.adapter.in.web.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.project.application.dto.request.UpdateProjectCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
	JsonNullable<@Size(min = 2, max = 100) @NotBlank String> name,
	JsonNullable<@Size(max = 255) String> description
) {
	public UpdateProjectCommand toCommand(String workspaceKey, String projectKey) {
		return UpdateProjectCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.name(name)
			.description(description)
			.build();
	}
}
