package com.tissue.issue.adapter.in.web.dto.request;

import java.time.Instant;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.issue.application.dto.request.UpdateCommonFieldsCommand;
import com.tissue.issue.domain.enums.IssuePriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommonFieldsRequest(
	JsonNullable<@NotBlank @Size(max = 100) String> title,
	JsonNullable<String> content,
	JsonNullable<String> summary,
	JsonNullable<IssuePriority> priority,
	JsonNullable<Instant> dueAt
) {
	public UpdateCommonFieldsCommand toCommand(String workspaceKey, String projectKey, String issueKey,
		Long actorMemberId) {
		return UpdateCommonFieldsCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.issueKey(issueKey)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueAt(dueAt)
			.actorMemberId(actorMemberId)
			.build();
	}
}
