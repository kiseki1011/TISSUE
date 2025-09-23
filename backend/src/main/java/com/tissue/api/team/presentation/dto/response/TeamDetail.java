package com.tissue.api.team.presentation.dto.response;

import java.time.Instant;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.team.domain.model.Team;

import lombok.Builder;

@Builder
public record TeamDetail(
	Long teamId,

	String name,
	String description,
	ColorType color,

	Instant createdAt,
	Instant updatedAt
) {
	public static TeamDetail from(Team team) {
		return TeamDetail.builder()
			.teamId(team.getId())
			.name(team.getName())
			.description(team.getDescription())
			.color(team.getColor())
			.createdAt(team.getCreatedAt())
			.updatedAt(team.getLastModifiedAt())
			.build();
	}
}
