package com.tissue.api.team.presentation.dto.response;

import java.util.List;

import com.tissue.api.team.domain.model.Team;

public record GetTeamsResponse(
	List<TeamDetail> teams
) {
	public static GetTeamsResponse from(List<Team> teams) {
		List<TeamDetail> responses = teams.stream()
			.map(TeamDetail::from)
			.toList();
		return new GetTeamsResponse(responses);
	}
}
