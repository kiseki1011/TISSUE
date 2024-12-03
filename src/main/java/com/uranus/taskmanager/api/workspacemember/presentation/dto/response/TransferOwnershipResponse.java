package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.WorkspaceMemberDetail;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class TransferOwnershipResponse {
	private WorkspaceMemberDetail requesterDetail;
	private WorkspaceMemberDetail targetDetail;

	public TransferOwnershipResponse(WorkspaceMemberDetail requesterDetail, WorkspaceMemberDetail targetDetail) {
		this.requesterDetail = requesterDetail;
		this.targetDetail = targetDetail;
	}

	public static TransferOwnershipResponse from(WorkspaceMember requester, WorkspaceMember target) {
		return new TransferOwnershipResponse(WorkspaceMemberDetail.from(requester), WorkspaceMemberDetail.from(target));
	}
}
