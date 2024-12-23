package com.tissue.api.issue.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;

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
