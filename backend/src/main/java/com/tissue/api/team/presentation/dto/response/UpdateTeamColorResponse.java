package com.tissue.api.team.presentation.dto.response;

import java.time.LocalDateTime;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.team.domain.Team;

public record UpdateTeamColorResponse(
	Long teamId,
	ColorType color,
	LocalDateTime updatedAt
) {
	public static UpdateTeamColorResponse from(Team team) {
		return new UpdateTeamColorResponse(
			team.getId(),
			team.getColor(),
			team.getLastModifiedDate()
		);
	}
}
