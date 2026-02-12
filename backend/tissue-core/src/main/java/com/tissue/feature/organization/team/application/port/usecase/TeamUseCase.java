package com.tissue.feature.organization.team.application.port.usecase;

import com.tissue.feature.organization.team.application.dto.request.CreateTeamCommand;
import com.tissue.feature.organization.team.application.dto.request.UpdateTeamCommand;
import com.tissue.feature.organization.team.application.dto.response.TeamCreateResponse;
import com.tissue.feature.organization.team.application.dto.response.TeamDetail;
import com.tissue.feature.organization.team.application.dto.response.TeamDetailList;

public interface TeamUseCase {

    TeamCreateResponse create(String workspaceKey, CreateTeamCommand cmd, Long memberId);

    void update(String workspaceKey, Long teamId, UpdateTeamCommand cmd, Long memberId);

    void delete(String workspaceKey, Long teamId, Long memberId);

    TeamDetail getTeam(String workspaceKey, Long teamId, Long memberId);

    TeamDetailList getWorkspaceTeams(String workspaceKey, Long memberId);

    // TODO: Team 검색 (pagination)
}
