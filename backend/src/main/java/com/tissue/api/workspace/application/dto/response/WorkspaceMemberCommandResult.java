package com.tissue.api.workspace.application.dto.response;

import com.tissue.api.workspace.domain.WorkspaceMember;

public record WorkspaceMemberCommandResult(
	String workspaceKey,
	Long memberId
) {
	public static WorkspaceMemberCommandResult from(WorkspaceMember workspaceMember) {
		return new WorkspaceMemberCommandResult(workspaceMember.getWorkspaceKey(), workspaceMember.getMember().getId());
	}
}
