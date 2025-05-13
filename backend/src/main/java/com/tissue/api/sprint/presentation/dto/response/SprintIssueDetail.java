package com.tissue.api.sprint.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;

import lombok.Builder;

@Builder
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
	public static SprintIssueDetail from(Issue issue) {
		return SprintIssueDetail.builder()
			.issueId(issue.getId())
			.issueKey(issue.getIssueKey())
			.type(issue.getType())
			.title(issue.getTitle())
			.status(issue.getStatus())
			.priority(issue.getPriority())
			.createdAt(issue.getCreatedDate())
			.createdBy(issue.getCreatedBy())
			.build();
	}
}
