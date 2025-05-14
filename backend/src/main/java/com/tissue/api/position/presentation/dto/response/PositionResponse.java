package com.tissue.api.position.presentation.dto.response;

import com.tissue.api.position.domain.model.Position;

public record PositionResponse(
	String workspaceCode,
	Long positionId
) {
	public static PositionResponse from(Position position) {
		return new PositionResponse(position.getWorkspaceCode(), position.getId());
	}
}
