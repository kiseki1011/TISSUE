package com.tissue.feature.organization.team.application.dto.response;

import com.tissue.feature.organization.team.domain.Team;
import java.util.List;

public record GetTeams(List<TeamDetail> teams) {
    public static GetTeams from(List<Team> teams) {
        List<TeamDetail> details = teams.stream().map(TeamDetail::from).toList();
        return new GetTeams(details);
    }
}
