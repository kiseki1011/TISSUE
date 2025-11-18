package com.tissue.api.workspace.application.dto.request;

public record AssignPositionCommand(
	String workspaceKey,
	Long memberId,
	Long positionId
) {
}
