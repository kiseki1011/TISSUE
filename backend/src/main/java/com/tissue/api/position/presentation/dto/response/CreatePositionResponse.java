package com.tissue.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.common.ColorType;
import com.tissue.api.position.domain.Position;

import lombok.Builder;

@Builder
public record CreatePositionResponse(
	Long positionId,
	String name,
	String description,
	ColorType color,
	LocalDateTime createdAt
) {
	public static CreatePositionResponse from(Position position) {
		return CreatePositionResponse.builder()
			.positionId(position.getId())
			.name(position.getName())
			.description(position.getDescription())
			.color(position.getColor())
			.createdAt(position.getCreatedDate())
			.build();
	}
}
