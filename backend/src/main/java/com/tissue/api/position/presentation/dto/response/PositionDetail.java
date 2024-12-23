package com.tissue.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.common.ColorType;
import com.tissue.api.position.domain.Position;

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
