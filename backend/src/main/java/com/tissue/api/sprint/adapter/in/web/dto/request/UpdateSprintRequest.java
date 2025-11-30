package com.tissue.api.sprint.adapter.in.web.dto.request;

import java.time.Instant;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.sprint.application.dto.request.UpdateSprintCommand;

import jakarta.validation.constraints.Size;

public record UpdateSprintRequest(
	JsonNullable<@Size(max = 50) String> title,
	JsonNullable<@Size(max = 255) String> goal,
	JsonNullable<Instant> startedAt,
	JsonNullable<Instant> dueAt
) {
	public UpdateSprintCommand toCommand(String workspaceKey, String projectKey, Long sprintId) {
		return UpdateSprintCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.sprintId(sprintId)
			.title(title)
			.goal(goal)
			.startedAt(startedAt)
			.dueAt(dueAt)
			.build();
	}
}
