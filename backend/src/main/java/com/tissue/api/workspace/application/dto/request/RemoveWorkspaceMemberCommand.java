package com.tissue.api.workspace.application.dto.request;

public record RemoveWorkspaceMemberCommand(
	String workspaceKey,
	Long targetMemberId,
	Long memberId
) {
}
