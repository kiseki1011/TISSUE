package com.tissue.project.adapter.in.web.dto.request;

import com.tissue.project.application.dto.request.CreateProjectCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
	@Size(min = 3, max = 12) @NotBlank String projectKey,
	@Size(min = 2, max = 100) @NotBlank String title,
	@Size(max = 255) String description
) {
	public CreateProjectCommand toCommand(String workspaceKey) {
		return CreateProjectCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.title(title)
			.description(description)
			.build();
	}
}
