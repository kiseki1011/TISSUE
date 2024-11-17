package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.WorkspaceMemberDetail;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class TransferWorkspaceOwnershipResponse {
	private WorkspaceMemberDetail workspaceMemberDetail;

	public TransferWorkspaceOwnershipResponse(WorkspaceMemberDetail workspaceMemberDetail) {
		this.workspaceMemberDetail = workspaceMemberDetail;
	}

	public static TransferWorkspaceOwnershipResponse from(WorkspaceMember workspaceMember) {
		return new TransferWorkspaceOwnershipResponse(WorkspaceMemberDetail.from(workspaceMember));
	}
}
