package com.tissue.api.issue.collaborator.presentation.dto.response;

import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.Builder;

@Builder
public record IssueReviewerResponse(
	String workspaceCode,
	String issueKey,
	Long reviewerMemberId
) {
	public static IssueReviewerResponse from(Issue issue, WorkspaceMember workspaceMember) {
		return IssueReviewerResponse.builder()
			.workspaceCode(issue.getWorkspaceCode())
			.issueKey(issue.getKey())
			.reviewerMemberId(workspaceMember.getMember().getId())
			.build();
	}
}
