package com.tissue.feature.organization.team.application.dto.response;

import com.tissue.feature.organization.team.domain.Team;
import java.util.List;

public record TeamDetailList(List<TeamDetail> teams) {

    public static TeamDetailList from(List<Team> teams) {
        List<TeamDetail> details = teams.stream().map(TeamDetail::from).toList();
        return new TeamDetailList(details);
    }
}
