package com.tissue.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.common.ColorType;
import com.tissue.api.position.domain.Position;

public record CreatePositionResponse(
	Long positionId,
	String name,
	String description,
	ColorType color,
	LocalDateTime createdAt
) {
	public static CreatePositionResponse from(Position position) {
		return new CreatePositionResponse(
			position.getId(),
			position.getName(),
			position.getDescription(),
			position.getColor(),
			position.getCreatedDate()
		);
	}
}
