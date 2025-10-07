package com.tissue.api.workspacemember.application.dto;

public record AssignPositionCommand(
	String workspaceKey,
	Long memberId,
	Long positionId
) {
}
