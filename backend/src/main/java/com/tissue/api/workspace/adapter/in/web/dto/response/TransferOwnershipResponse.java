package com.tissue.api.workspace.adapter.in.web.dto.response;

public record TransferOwnershipResponse(
	String workspaceCode,
	Long requesterMemberId,
	Long targetMemberId
) {
	// public static TransferOwnershipResponse from(WorkspaceMember requester, WorkspaceMember target) {
	// 	return new TransferOwnershipResponse(
	// 		target.getWorkspaceCode(),
	// 		requester.getMember().getId(),
	// 		target.getMember().getId()
	// 	);
	// }
}
