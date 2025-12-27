package com.tissue.team.application.dto.response;

import com.tissue.team.domain.Team;

public record TeamCreateResponse(String workspaceKey, Long teamId) {
    public static TeamCreateResponse from(Team team) {
        return new TeamCreateResponse(team.getWorkspaceKey(), team.getId());
    }
}
