package com.tissue.api.workspacemember.application.dto;

public record TransferOwnershipCommand(
	String workspaceKey,
	Long targetMemberId,
	Long memberId
) {
}
