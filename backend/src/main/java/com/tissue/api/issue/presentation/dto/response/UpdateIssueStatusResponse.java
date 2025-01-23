package com.tissue.api.issue.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;

import lombok.Builder;

@Builder
public record UpdateIssueStatusResponse(

	Long issueId,
	String issueKey,
	IssueStatus status,
	LocalDateTime updatedAt

) {

	public static UpdateIssueStatusResponse from(Issue issue) {
		return UpdateIssueStatusResponse.builder()
			.issueId(issue.getId())
			.issueKey(issue.getIssueKey())
			.status(issue.getStatus())
			.updatedAt(issue.getLastModifiedDate())
			.build();
	}
}
