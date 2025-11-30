package com.tissue.api.workspace.application.dto.request;

public record AddPositionCommand(
	String workspaceKey,
	Long actorMemberId,
	Long targetMemberId,
	Long positionId
) {
}
