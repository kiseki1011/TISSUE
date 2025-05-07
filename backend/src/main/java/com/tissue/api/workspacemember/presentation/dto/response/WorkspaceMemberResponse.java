package com.tissue.api.workspacemember.presentation.dto.response;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

public record WorkspaceMemberResponse(
	String workspaceCode,
	Long memberId
) {
	public static WorkspaceMemberResponse from(WorkspaceMember workspaceMember) {
		return new WorkspaceMemberResponse(workspaceMember.getWorkspaceCode(), workspaceMember.getMember().getId());
	}
}
