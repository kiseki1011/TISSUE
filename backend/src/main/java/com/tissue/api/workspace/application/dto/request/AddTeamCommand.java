package com.tissue.api.workspace.application.dto.request;

public record AddTeamCommand(
	String workspaceKey,
	Long actorMemberId,
	Long targetMemberId,
	Long teamId
) {
}
