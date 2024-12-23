package com.tissue.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.common.ColorType;
import com.tissue.api.position.domain.Position;

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
