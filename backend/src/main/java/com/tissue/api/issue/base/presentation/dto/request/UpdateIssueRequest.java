package com.tissue.api.issue.base.presentation.dto.request;

import java.time.LocalDateTime;
import java.util.Map;

import com.tissue.api.issue.base.application.dto.UpdateIssueCommand;
import com.tissue.api.issue.base.domain.enums.IssuePriority;

// TODO: Add validation annotations
public record UpdateIssueRequest(
	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDateTime dueAt,
	Map<String, Object> customFields
) {
	public UpdateIssueCommand toCommand(String workspaceCode, String issueKey) {
		return UpdateIssueCommand.builder()
			.workspaceCode(workspaceCode)
			.issueKey(issueKey)
			.title(title)
			.content(content)
			.summary(summary)
			.priority(priority)
			.dueAt(dueAt)
			.customFields(customFields)
			.build();
	}
}
