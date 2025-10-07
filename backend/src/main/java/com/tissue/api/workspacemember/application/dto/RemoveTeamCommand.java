package com.tissue.api.workspacemember.application.dto;

public record RemoveTeamCommand(
	String workspaceKey,
	Long memberId,
	Long teamId
) {
}
