package com.tissue.api.issue.presentation.dto.request;

import java.time.Instant;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.issue.application.dto.UpdateCommonFieldsCommand;
import com.tissue.api.issue.domain.enums.IssuePriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommonFieldsRequest(
	JsonNullable<@NotBlank @Size(max = 100) String> title,
	JsonNullable<String> content,
	JsonNullable<String> summary,
	JsonNullable<IssuePriority> priority,
	JsonNullable<Instant> dueAt,
	JsonNullable<Integer> storyPoint
) {
	public UpdateCommonFieldsCommand toCommand(String workspaceKey, String issueKey) {
		return UpdateCommonFieldsCommand.builder()
			.workspaceKey(workspaceKey)
			.issueKey(issueKey)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueAt(dueAt)
			.storyPoint(storyPoint)
			.build();
	}
}
