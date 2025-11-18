package com.tissue.api.workspace.application.dto.request;

public record RemovePositionCommand(
	String workspaceKey,
	Long memberId,
	Long positionId
) {
}
