package com.tissue.workspace.application.dto.in;

import lombok.Builder;

@Builder
public record ManagePositionCommand(
	String workspaceKey,
	Long memberId,
	Long positionId,
	Long actorMemberId
) {
}
