package com.tissue.team.application.dto.response;

import java.util.List;

import com.tissue.team.domain.Team;

public record GetTeams(
	List<TeamDetail> teams
) {
	public static GetTeams from(List<Team> teams) {
		List<TeamDetail> details = teams.stream()
			.map(TeamDetail::from)
			.toList();
		return new GetTeams(details);
	}
}
