package com.tissue.workspace.application.dto.response;

import com.tissue.workspace.domain.WorkspaceMember;

public record WorkspaceMemberCommandResponse(
	String workspaceKey,
	Long memberId
) {
	public static WorkspaceMemberCommandResponse from(WorkspaceMember workspaceMember) {
		return new WorkspaceMemberCommandResponse(workspaceMember.getWorkspaceKey(), workspaceMember.getMemberId());
	}
}
