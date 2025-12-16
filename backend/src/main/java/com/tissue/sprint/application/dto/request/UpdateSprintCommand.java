package com.tissue.sprint.application.dto.request;

import java.time.Instant;

import org.openapitools.jackson.nullable.JsonNullable;

import lombok.Builder;

@Builder
public record UpdateSprintCommand(
	String workspaceKey,
	String projectKey,
	Long sprintId,
	JsonNullable<String> title,
	JsonNullable<String> goal,
	JsonNullable<Instant> startedAt,
	JsonNullable<Instant> dueAt
) {
}
