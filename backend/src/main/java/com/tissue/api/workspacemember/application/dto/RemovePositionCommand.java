package com.tissue.api.workspacemember.application.dto;

public record RemovePositionCommand(
	String workspaceKey,
	Long memberId,
	Long positionId
) {
}
