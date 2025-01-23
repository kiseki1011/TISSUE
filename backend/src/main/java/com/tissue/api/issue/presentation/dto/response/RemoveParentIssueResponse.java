package com.tissue.api.issue.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.issue.domain.Issue;

import lombok.Builder;

@Builder
public record RemoveParentIssueResponse(

	Long issueId,
	String issueKey,
	LocalDateTime removedAt

) {

	public static RemoveParentIssueResponse from(Issue issue) {
		return RemoveParentIssueResponse.builder()
			.issueId(issue.getId())
			.issueKey(issue.getIssueKey())
			.removedAt(issue.getLastModifiedDate())
			.build();
	}
}
