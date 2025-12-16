package com.tissue.team.presentation.dto.response;

import com.tissue.team.domain.model.Team;

public record TeamResponse(
	String workspaceCode,
	Long teamId
) {
	public static TeamResponse from(Team team) {
		return new TeamResponse(team.getWorkspaceKey(), team.getId());
	}
}
