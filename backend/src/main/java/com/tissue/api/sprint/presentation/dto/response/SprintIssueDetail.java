package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;

public record SprintIssueDetail(
	Long issueId,
	String issueKey,
	IssueType type,
	String title,
	IssueStatus status,
	IssuePriority priority,
	LocalDateTime createdAt,
	Long createdBy
) {
}
