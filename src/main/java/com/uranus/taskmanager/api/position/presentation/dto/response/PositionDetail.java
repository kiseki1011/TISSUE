package com.uranus.taskmanager.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.position.domain.Position;

public record PositionDetail(
	Long id,
	String name,
	String description,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static PositionDetail from(Position position) {
		return new PositionDetail(
			position.getId(),
			position.getName(),
			position.getDescription(),
			position.getCreatedDate(),
			position.getLastModifiedDate()
		);
	}
}
