package com.tissue.api.workspace.application.dto.request;

public record AssignTeamCommand(
	String workspaceKey,
	Long memberId,
	Long teamId
) {
}
