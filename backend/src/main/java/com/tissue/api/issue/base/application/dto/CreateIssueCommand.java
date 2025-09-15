package com.tissue.api.issue.base.application.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.tissue.api.issue.base.domain.enums.IssuePriority;

import lombok.Builder;

@Builder
public record CreateIssueCommand(
	String workspaceKey,
	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDateTime dueAt,
	Long issueTypeId,
	Map<Long, Object> customFields
) {
}
