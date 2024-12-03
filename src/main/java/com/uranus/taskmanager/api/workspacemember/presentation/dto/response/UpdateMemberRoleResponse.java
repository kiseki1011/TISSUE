package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.WorkspaceMemberDetail;

import lombok.Getter;

@Getter
public class UpdateMemberRoleResponse {
	private WorkspaceMemberDetail targetDetail;

	public UpdateMemberRoleResponse(WorkspaceMemberDetail targetDetail) {
		this.targetDetail = targetDetail;
	}

	public static UpdateMemberRoleResponse from(WorkspaceMember workspaceMember) {
		return new UpdateMemberRoleResponse(WorkspaceMemberDetail.from(workspaceMember));
	}
}
