package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.WorkspaceMemberDetail;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class TransferOwnershipResponse {
	private WorkspaceMemberDetail workspaceMemberDetail;

	public TransferOwnershipResponse(WorkspaceMemberDetail workspaceMemberDetail) {
		this.workspaceMemberDetail = workspaceMemberDetail;
	}

	public static TransferOwnershipResponse from(WorkspaceMember workspaceMember) {
		return new TransferOwnershipResponse(WorkspaceMemberDetail.from(workspaceMember));
	}
}
