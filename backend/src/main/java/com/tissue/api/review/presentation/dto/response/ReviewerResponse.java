package com.tissue.api.review.presentation.dto.response;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;

@Builder
public record ReviewerResponse(
	String workspaceCode,
	String issueKey,
	Long reviewerMemberId
) {
	public static ReviewerResponse from(Issue issue, WorkspaceMember workspaceMember) {
		return ReviewerResponse.builder()
			.workspaceCode(issue.getWorkspaceCode())
			.issueKey(issue.getIssueKey())
			.reviewerMemberId(workspaceMember.getMember().getId())
			.build();
	}
}
