package com.tissue.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.workspacemember.domain.WorkspaceMember;

public record TransferOwnershipResponse(
	Long requesterWorkspaceMemberId,
	Long targetWorkspaceMemberId,
	LocalDateTime ownershipTransferredAt
) {
	public static TransferOwnershipResponse from(WorkspaceMember requester, WorkspaceMember target) {
		return new TransferOwnershipResponse(
			requester.getId(),
			target.getId(),
			requester.getLastModifiedDate()
		);
	}
}
