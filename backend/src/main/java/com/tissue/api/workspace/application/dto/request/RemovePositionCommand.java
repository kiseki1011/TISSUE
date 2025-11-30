package com.tissue.api.workspace.application.dto.request;

public record RemovePositionCommand(
	String workspaceKey,
	Long actorMemberId,
	Long targetMemberId,
	Long positionId
) {
}
