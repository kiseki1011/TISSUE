package com.uranus.taskmanager.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.position.domain.Position;

public record DeletePositionResponse(
	Long positionId,
	LocalDateTime deletedAt
) {
	public static DeletePositionResponse from(Position position) {
		return new DeletePositionResponse(
			position.getId(),
			LocalDateTime.now()
		);
	}
}
