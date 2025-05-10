package com.tissue.api.issue.presentation.dto.response;

import com.tissue.api.issue.domain.Issue;

import lombok.Builder;

@Builder
public record WatchIssueResponse(
	String workspaceCode,
	String issueKey,
	Long memberId
) {
	public static WatchIssueResponse from(Issue issue, Long memberId) {
		return WatchIssueResponse.builder()
			.workspaceCode(issue.getWorkspaceCode())
			.issueKey(issue.getIssueKey())
			.memberId(memberId)
			.build();
	}
}
