package com.tissue.api.team.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.team.domain.Team;

public record DeleteTeamResponse(
	Long teamId,
	LocalDateTime deletedAt
) {
	public static DeleteTeamResponse from(Team team) {
		return new DeleteTeamResponse(
			team.getId(),
			LocalDateTime.now()
		);
	}
}
