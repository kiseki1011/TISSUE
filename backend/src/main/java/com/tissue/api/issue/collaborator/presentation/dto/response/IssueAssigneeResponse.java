package com.tissue.api.issue.collaborator.presentation.dto.response;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.Builder;

@Builder
public record IssueAssigneeResponse(
	String workspaceCode,
	String issueKey,
	Long memberId
) {
	public static IssueAssigneeResponse from(WorkspaceMember assignee, String issueKey) {
		return IssueAssigneeResponse.builder()
			.workspaceCode(assignee.getWorkspaceKey())
			.issueKey(issueKey)
			.memberId(assignee.getMember().getId())
			.build();
	}
}
