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
	String issueTypeKey,
	Map<String, Object> customFields
) {
	public CreateIssueCommand toCommand(String workspaceCode) {
		return CreateIssueCommand.builder()
			.workspaceCode(workspaceCode)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueAt(dueAt)
			.issueTypeKey(issueTypeKey)
			.customFields(customFields)
			.build();
	}
}
