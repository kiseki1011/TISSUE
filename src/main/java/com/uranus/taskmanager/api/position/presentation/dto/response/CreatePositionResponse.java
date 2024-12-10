package com.uranus.taskmanager.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.position.domain.Position;

public record CreatePositionResponse(
	Long id,
	String name,
	String description,
	LocalDateTime createdAt
) {
	public static CreatePositionResponse from(Position position) {
		return new CreatePositionResponse(
			position.getId(),
			position.getName(),
			position.getDescription(),
			position.getCreatedDate()
		);
	}
}
