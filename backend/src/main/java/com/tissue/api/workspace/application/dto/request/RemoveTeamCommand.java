package com.tissue.api.workspace.application.dto.request;

public record RemoveTeamCommand(
	String workspaceKey,
	Long memberId,
	Long teamId
) {
}
