package com.tissue.api.workspace.application.dto.request;

public record TransferOwnershipCommand(
	String workspaceKey,
	Long targetMemberId,
	Long memberId
) {
}
