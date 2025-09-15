package com.tissue.api.issue.base.presentation.dto.request;

import java.time.LocalDateTime;
import java.util.Map;

import com.tissue.api.issue.base.application.dto.CreateIssueCommand;
import com.tissue.api.issue.base.domain.enums.IssuePriority;

public record CreateIssueRequest(
	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDateTime dueAt,
	Long issueTypeId,
	Map<Long, Object> customFields
) {
	public CreateIssueCommand toCommand(String workspaceKey) {
		return CreateIssueCommand.builder()
			.workspaceKey(workspaceKey)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueAt(dueAt)
			.issueTypeId(issueTypeId)
			.customFields(customFields)
			.build();
	}
}
