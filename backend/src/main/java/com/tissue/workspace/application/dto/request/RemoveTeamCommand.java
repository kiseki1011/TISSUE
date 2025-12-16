package com.tissue.workspace.application.dto.request;

public record RemoveTeamCommand(
	String workspaceKey,
	Long actorMemberId,
	Long targetMemberId,
	Long teamId
) {
}
