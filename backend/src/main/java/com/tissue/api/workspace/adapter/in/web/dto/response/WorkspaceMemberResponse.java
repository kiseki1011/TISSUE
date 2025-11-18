package com.tissue.api.workspace.adapter.in.web.dto.response;

import com.tissue.api.workspace.domain.WorkspaceMember;

public record WorkspaceMemberResponse(
	String workspaceKey,
	Long memberId
) {
	public static WorkspaceMemberResponse from(WorkspaceMember workspaceMember) {
		return new WorkspaceMemberResponse(workspaceMember.getWorkspaceKey(), workspaceMember.getMember().getId());
	}
}
