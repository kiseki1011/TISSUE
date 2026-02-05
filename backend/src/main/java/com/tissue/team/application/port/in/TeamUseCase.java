package com.tissue.team.application.port.in;

import com.tissue.team.application.dto.request.CreateTeamCommand;
import com.tissue.team.application.dto.request.UpdateTeamCommand;
import com.tissue.team.application.dto.response.GetTeams;
import com.tissue.team.application.dto.response.TeamCreateResponse;
import com.tissue.team.application.dto.response.TeamDetail;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public interface TeamUseCase {

    TeamCreateResponse create(CreateTeamCommand cmd, WorkspaceMemberContext actorContext);

    void update(Long teamId, UpdateTeamCommand cmd, WorkspaceMemberContext actorContext);

    void delete(Long teamId, WorkspaceMemberContext actorContext);

    TeamDetail getTeam(Long teamId, WorkspaceMemberContext actorContext);

    GetTeams getTeams(WorkspaceMemberContext actorContext);
}
