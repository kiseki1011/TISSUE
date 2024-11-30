package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.WorkspaceMemberDetail;

import lombok.Getter;

@Getter
public class UpdateMemberRoleResponse {
	private WorkspaceMemberDetail workspaceMemberDetail;

	public UpdateMemberRoleResponse(WorkspaceMemberDetail workspaceMemberDetail) {
		this.workspaceMemberDetail = workspaceMemberDetail;
	}

	public static UpdateMemberRoleResponse from(WorkspaceMember workspaceMember) {
		return new UpdateMemberRoleResponse(WorkspaceMemberDetail.from(workspaceMember));
	}
}
