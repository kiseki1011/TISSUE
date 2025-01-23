package com.tissue.api.team.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.team.domain.Team;

import lombok.Builder;

@Builder
public record UpdateTeamResponse(
	Long teamId,

	String name,
	String description,

	LocalDateTime updatedAt
) {
	public static UpdateTeamResponse from(Team team) {
		return UpdateTeamResponse.builder()
			.teamId(team.getId())
			.name(team.getName())
			.description(team.getDescription())
			.updatedAt(team.getLastModifiedDate())
			.build();
	}
}
