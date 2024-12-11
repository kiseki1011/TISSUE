package com.uranus.taskmanager.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.position.domain.Position;

public record UpdatePositionResponse(
	Long id,
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
