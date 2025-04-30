package com.tissue.api.assignee.presentation.dto.response;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;

@Builder
public record IssueAssigneeResponse(
	String workspaceCode,
	String issueKey,
	Long memberId
) {
	public static IssueAssigneeResponse from(WorkspaceMember assignee, String issueKey) {
		return IssueAssigneeResponse.builder()
			.workspaceCode(assignee.getWorkspaceCode())
			.issueKey(issueKey)
			.memberId(assignee.getMember().getId())
			.build();
	}
}
