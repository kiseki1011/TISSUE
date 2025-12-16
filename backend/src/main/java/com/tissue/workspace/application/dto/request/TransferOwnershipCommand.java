package com.tissue.workspace.application.dto.request;

public record TransferOwnershipCommand(
	String workspaceKey,
	Long actorMemberId,
	Long targetMemberId
) {
}
