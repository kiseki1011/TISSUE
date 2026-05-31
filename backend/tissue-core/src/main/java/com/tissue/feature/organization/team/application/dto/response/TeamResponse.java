package com.tissue.feature.organization.team.application.dto.response;

import com.tissue.feature.organization.team.domain.Team;

public record TeamResponse(Long teamId) {

    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getId());
    }
}
