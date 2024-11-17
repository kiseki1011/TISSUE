package com.uranus.taskmanager.api.invitation.presentation.dto.response;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class InvitationAcceptResponse {
	private WorkspaceDetail workspaceDetail;
	private String nickname;

	@Builder
	public InvitationAcceptResponse(WorkspaceDetail workspaceDetail, String nickname) {
		this.workspaceDetail = workspaceDetail;
		this.nickname = nickname;
	}

	public static InvitationAcceptResponse from(Workspace workspace, WorkspaceRole role) {
		return InvitationAcceptResponse.builder()
			.workspaceDetail(WorkspaceDetail.from(workspace, role))
			.build();
	}
}
