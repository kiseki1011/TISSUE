package com.tissue.feature.organization.team.application.port.usecase;

import com.tissue.feature.organization.team.application.dto.request.CreateTeamCommand;
import com.tissue.feature.organization.team.application.dto.request.UpdateTeamCommand;
import com.tissue.feature.organization.team.application.dto.response.TeamCreateResponse;
import com.tissue.feature.organization.team.application.dto.response.TeamDetail;
import com.tissue.feature.organization.team.application.dto.response.TeamDetailList;

public interface TeamUseCase {

    TeamCreateResponse create(String workspaceKey, CreateTeamCommand cmd, Long actorMemberId);

    void update(String workspaceKey, Long teamId, UpdateTeamCommand cmd, Long actorMemberId);

    void delete(String workspaceKey, Long teamId, Long actorMemberId);

    TeamDetail getTeam(String workspaceKey, Long teamId, Long actorMemberId);

    TeamDetailList getWorkspaceTeams(String workspaceKey, Long actorMemberId);

    // TODO: Team 검색 (pagination)
}
