package com.tissue.api.issue.application.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.tissue.api.issue.domain.enums.IssuePriority;

import lombok.Builder;

@Builder
public record CreateIssueCommand(
	Long currentMemberId,
	String workspaceKey,
	String title,
	String content,
	String summary,
	IssuePriority priority,
	LocalDateTime dueAt,
	Integer storyPoint,
	Long issueTypeId,
	Map<Long, Object> customFields
) {
}
