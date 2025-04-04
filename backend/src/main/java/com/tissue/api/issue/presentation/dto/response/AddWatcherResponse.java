package com.tissue.api.issue.presentation.dto.response;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;

@Builder
public record AddWatcherResponse(
	Long watcherWorkspaceMemberId,
	String watcherNickname,
	String issueKey,
	String workspaceCode
) {
	public static AddWatcherResponse from(WorkspaceMember workspaceMember, Issue issue) {
		return AddWatcherResponse.builder()
			.watcherWorkspaceMemberId(workspaceMember.getId())
			.watcherNickname(workspaceMember.getNickname())
			.issueKey(issue.getIssueKey())
			.workspaceCode(issue.getWorkspaceCode())
			.build();
	}
}
