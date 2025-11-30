package com.tissue.api.sprint.adapter.in.web.dto.request;

import com.tissue.api.sprint.application.dto.request.CreateSprintCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSprintRequest(
	@Size(max = 50) @NotBlank String title,
	@Size(max = 255) String goal
) {
	public CreateSprintCommand toCommand(String workspaceKey, String projectKey) {
		return CreateSprintCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.title(title)
			.goal(goal)
			.build();
	}
}
