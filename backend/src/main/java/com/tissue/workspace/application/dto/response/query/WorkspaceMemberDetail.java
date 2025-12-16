package com.tissue.workspace.application.dto.response.query;

import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;

import lombok.Builder;

@Builder
public record WorkspaceMemberDetail(
	String workspaceKey,
	Long memberId,
	String displayName,
	String userName,
	// TODO: name -> workspaceMember.getMember().getName()
	WorkspaceRole workspaceRole
) {
	public static WorkspaceMemberDetail from(WorkspaceMember workspaceMember) {
		return WorkspaceMemberDetail.builder()
			.workspaceKey(workspaceMember.getWorkspaceKey())
			.memberId(workspaceMember.getMemberId())
			.displayName(workspaceMember.getDisplayName())
			.userName(workspaceMember.getMember().getUsername())
			.workspaceRole(workspaceMember.getRole())
			.build();
	}
}
