package com.tissue.api.project.adapter.in.web.dto.request;

import com.tissue.api.project.application.dto.request.CreateProjectCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
	// TODO: max 12 적당한가?
	@Size(min = 3, max = 12) @NotBlank String projectKey,
	@Size(min = 2, max = 100) @NotBlank String name,
	@Size(max = 255) String description
) {
	public CreateProjectCommand toCommand(String workspaceKey) {
		return CreateProjectCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.name(name)
			.description(description)
			.build();
	}
}
