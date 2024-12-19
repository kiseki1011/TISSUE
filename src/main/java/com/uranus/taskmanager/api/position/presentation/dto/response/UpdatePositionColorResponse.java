package com.uranus.taskmanager.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.common.ColorType;
import com.uranus.taskmanager.api.position.domain.Position;

public record UpdatePositionColorResponse(
	Long positionId,
	ColorType color,
	LocalDateTime updatedAt
) {
	public static UpdatePositionColorResponse from(Position position) {
		return new UpdatePositionColorResponse(
			position.getId(),
			position.getColor(),
			position.getLastModifiedDate()
		);
	}
}
