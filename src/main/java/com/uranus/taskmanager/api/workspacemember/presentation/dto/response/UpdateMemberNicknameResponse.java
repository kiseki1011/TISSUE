package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.WorkspaceMemberDetail;

import lombok.Getter;

@Getter
public class UpdateMemberNicknameResponse {
	private WorkspaceMemberDetail updatedDetail;

	public UpdateMemberNicknameResponse(WorkspaceMemberDetail updatedDetail) {
		this.updatedDetail = updatedDetail;
	}

	public static UpdateMemberNicknameResponse from(WorkspaceMember workspaceMember) {
		return new UpdateMemberNicknameResponse(WorkspaceMemberDetail.from(workspaceMember));
	}
}
