package com.tissue.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.position.domain.Position;

public record UpdatePositionResponse(
	Long positionId,
	String name,
	String description,
	LocalDateTime updatedAt
) {
	public static UpdatePositionResponse from(Position position) {
		return new UpdatePositionResponse(
			position.getId(),
			position.getName(),
			position.getDescription(),
			position.getLastModifiedDate()
		);
	}
}
