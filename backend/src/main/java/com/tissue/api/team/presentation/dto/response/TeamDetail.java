package com.tissue.api.team.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.team.domain.Team;

import lombok.Builder;

@Builder
public record TeamDetail(
	Long teamId,
	String name,
	String description,
	ColorType color,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static TeamDetail from(Team team) {
		return TeamDetail.builder()
			.teamId(team.getId())
			.name(team.getName())
			.description(team.getDescription())
			.color(team.getColor())
			.createdAt(team.getCreatedDate())
			.updatedAt(team.getLastModifiedDate())
			.build();
	}
}
