package com.tissue.issue.application.dto.request;

import java.time.Instant;
import java.util.Map;

import com.tissue.issue.domain.enums.IssuePriority;

import lombok.Builder;

@Builder
public record CreateIssueCommand(
	String workspaceKey,
	String projectKey,
	Long sprintId,
	String parentProjectKey,
	String parentKey,
	String title,
	String content,
	String summary,
	IssuePriority priority,
	Instant dueAt,
	Integer storyPoint,
	Long issueTypeId,
	Map<Long, Object> customFields,
	Long assigneeMemberId,
	Long actorMemberId
) {
}
