package com.tissue.api.workspacemember.application.dto;

public record AssignTeamCommand(
	String workspaceKey,
	Long memberId,
	Long teamId
) {
}
