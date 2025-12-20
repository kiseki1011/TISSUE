package com.tissue.sprint.adapter.in.web.dto.request;

import java.time.Instant;

import com.tissue.sprint.application.dto.request.StartSprintCommand;

import lombok.NonNull;

public record StartSprintRequest(
	@NonNull Instant dueAt
) {
	public StartSprintCommand toCommand(String workspaceKey, String projectKey, Long sprintId) {
		return StartSprintCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.sprintId(sprintId)
			.dueAt(dueAt)
			.build();
	}
}
