package com.tissue.feature.organization.team.application.port.usecase;

import com.tissue.feature.organization.team.application.dto.request.CreateTeamCommand;
import com.tissue.feature.organization.team.application.dto.request.UpdateTeamCommand;
import com.tissue.feature.organization.team.application.dto.response.TeamCreateResponse;
import com.tissue.feature.organization.team.application.dto.response.TeamDetail;
import com.tissue.feature.organization.team.application.dto.response.TeamDetailList;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;

public interface TeamUseCase {

    TeamCreateResponse create(CreateTeamCommand cmd, WorkspaceMemberContext actorContext);

    void update(Long teamId, UpdateTeamCommand cmd, WorkspaceMemberContext actorContext);

    void delete(Long teamId, WorkspaceMemberContext actorContext);

    TeamDetail getTeam(Long teamId, WorkspaceMemberContext actorContext);

    TeamDetailList getWorkspaceTeams(WorkspaceMemberContext actorContext);

    // TODO: Team 검색 (pagination)
}
