package com.tissue.workspace.application.dto.request;

public record RemovePositionCommand(
	String workspaceKey,
	Long actorMemberId,
	Long targetMemberId,
	Long positionId
) {
}
