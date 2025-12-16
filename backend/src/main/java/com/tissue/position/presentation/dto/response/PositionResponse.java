package com.tissue.position.presentation.dto.response;

import com.tissue.position.domain.model.Position;

public record PositionResponse(
	String workspaceCode,
	Long positionId
) {
	public static PositionResponse from(Position position) {
		return new PositionResponse(position.getWorkspaceKey(), position.getId());
	}
}
