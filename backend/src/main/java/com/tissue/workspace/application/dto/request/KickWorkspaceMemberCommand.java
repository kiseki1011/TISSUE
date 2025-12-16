package com.tissue.workspace.application.dto.request;

public record KickWorkspaceMemberCommand(
	String workspaceKey,
	Long targetMemberId,
	Long actorMemberId
) {
}
