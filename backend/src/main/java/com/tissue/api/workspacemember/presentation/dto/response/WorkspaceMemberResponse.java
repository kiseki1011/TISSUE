package com.tissue.api.workspacemember.presentation.dto.response;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

public record WorkspaceMemberResponse(
	String workspaceKey,
	Long memberId
) {
	public static WorkspaceMemberResponse from(WorkspaceMember workspaceMember) {
		return new WorkspaceMemberResponse(workspaceMember.getWorkspaceKey(), workspaceMember.getMember().getId());
	}
}
