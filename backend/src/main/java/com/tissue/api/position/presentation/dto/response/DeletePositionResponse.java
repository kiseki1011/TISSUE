package com.tissue.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.position.domain.Position;

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
