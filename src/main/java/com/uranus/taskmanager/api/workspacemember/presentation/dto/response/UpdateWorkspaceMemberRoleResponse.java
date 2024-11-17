package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.WorkspaceMemberDetail;

import lombok.Getter;

@Getter
public class UpdateWorkspaceMemberRoleResponse {
	private WorkspaceMemberDetail workspaceMemberDetail;

	public UpdateWorkspaceMemberRoleResponse(WorkspaceMemberDetail workspaceMemberDetail) {
		this.workspaceMemberDetail = workspaceMemberDetail;
	}

	public static UpdateWorkspaceMemberRoleResponse from(WorkspaceMember workspaceMember) {
		return new UpdateWorkspaceMemberRoleResponse(WorkspaceMemberDetail.from(workspaceMember));
	}
}
