package com.tissue.api.position.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.position.domain.Position;

import lombok.Builder;

@Builder
public record UpdatePositionResponse(
	Long positionId,
	String name,
	String description,
	LocalDateTime updatedAt
) {
	public static UpdatePositionResponse from(Position position) {
		return UpdatePositionResponse.builder()
			.positionId(position.getId())
			.name(position.getName())
			.description(position.getDescription())
			.updatedAt(position.getLastModifiedDate())
			.build();
	}
}
