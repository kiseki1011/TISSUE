package com.uranus.taskmanager.api.issue.presentation.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueStatus;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;

public record CreateIssueResponse(
	Long issueId,
	IssueType type,
	String title,
	String content,
	IssuePriority priority,
	IssueStatus status,
	LocalDateTime createdAt,
	LocalDate dueDate,
	Long parentIssueId
) {
	public static CreateIssueResponse from(Issue issue) {
		return new CreateIssueResponse(
			issue.getId(),
			issue.getType(),
			issue.getTitle(),
			issue.getContent(),
			issue.getPriority(),
			issue.getStatus(),
			issue.getCreatedDate(),
			issue.getDueDate(),
			Optional.ofNullable(issue.getParentIssue())
				.map(Issue::getId)
				.orElse(null)
		);
	}
}
