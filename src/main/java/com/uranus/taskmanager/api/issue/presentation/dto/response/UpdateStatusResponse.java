package com.uranus.taskmanager.api.issue.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.IssueStatus;

public record UpdateStatusResponse(
	Long issueId,
	IssueStatus status,
	LocalDateTime updatedAt
) {
	public static UpdateStatusResponse from(Issue issue) {
		return new UpdateStatusResponse(
			issue.getId(),
			issue.getStatus(),
			issue.getLastModifiedDate()
		);
	}
}
