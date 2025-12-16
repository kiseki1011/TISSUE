package com.tissue.position.presentation.dto.response;

import java.time.Instant;

import com.tissue.common.enums.ColorType;
import com.tissue.position.domain.model.Position;

import lombok.Builder;

@Builder
public record PositionDetail(
	Long positionId,

	String name,
	String description,
	ColorType color,

	Instant createdAt,
	Instant updatedAt
) {
	public static PositionDetail from(Position position) {
		return PositionDetail.builder()
			.positionId(position.getId())
			.name(position.getName())
			.description(position.getDescription())
			.color(position.getColor())
			.createdAt(position.getCreatedAt())
			.updatedAt(position.getLastModifiedAt())
			.build();
	}
}
