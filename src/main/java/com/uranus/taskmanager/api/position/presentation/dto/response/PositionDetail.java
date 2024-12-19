package com.uranus.taskmanager.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.common.ColorType;
import com.uranus.taskmanager.api.position.domain.Position;

public record PositionDetail(
	Long positionId,
	String name,
	String description,
	ColorType color,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static PositionDetail from(Position position) {
		return new PositionDetail(
			position.getId(),
			position.getName(),
			position.getDescription(),
			position.getColor(),
			position.getCreatedDate(),
			position.getLastModifiedDate()
		);
	}
}
