package com.tissue.api.issue.application.dto.request;

import java.time.Instant;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.issue.domain.enums.IssuePriority;

import lombok.Builder;

@Builder
public record UpdateCommonFieldsCommand(
	String workspaceKey,
	String projectKey,
	String issueKey,
	JsonNullable<String> title,
	JsonNullable<String> content,
	JsonNullable<String> summary,
	JsonNullable<IssuePriority> priority,
	JsonNullable<Instant> dueAt
) {
}
