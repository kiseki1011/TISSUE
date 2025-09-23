package com.tissue.api.sprint.presentation.dto.response;

import java.time.Instant;

import com.tissue.api.issue.base.domain.enums.IssuePriority;
import com.tissue.api.issue.base.domain.model.Issue;

import lombok.Builder;

@Builder
public record SprintIssueDetail(
	Long issueId,
	String issueKey,
	// IssueType type,
	String title,
	// IssueStatus status,
	IssuePriority priority,
	Instant createdAt,
	Long createdBy
) {
	public static SprintIssueDetail from(Issue issue) {
		return SprintIssueDetail.builder()
			.issueId(issue.getId())
			.issueKey(issue.getKey())
			// .type(issue.getType())
			.title(issue.getTitle())
			// .status(issue.getStatus())
			.priority(issue.getPriority())
			.createdAt(issue.getCreatedAt())
			.createdBy(issue.getCreatedBy())
			.build();
	}
}
