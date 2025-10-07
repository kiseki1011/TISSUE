package com.tissue.api.workspacemember.application.dto;

public record RemoveWorkspaceMemberCommand(
	String workspaceKey,
	Long targetMemberId,
	Long memberId
) {
}
