package com.tissue.sprint.application.dto.request;

import java.time.Instant;

import lombok.Builder;

@Builder
public record StartSprintCommand(
	String workspaceKey,
	String projectKey,
	Long sprintId,
	Instant dueAt
) {
}
