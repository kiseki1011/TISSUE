package com.tissue.api.workspacemember.presentation.dto.response;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

public record TransferOwnershipResponse(
	String workspaceCode,
	Long requesterMemberId,
	Long targetMemberId
) {
	public static TransferOwnershipResponse from(WorkspaceMember requester, WorkspaceMember target) {
		return new TransferOwnershipResponse(
			target.getWorkspaceCode(),
			requester.getMember().getId(),
			target.getMember().getId()
		);
	}
}
