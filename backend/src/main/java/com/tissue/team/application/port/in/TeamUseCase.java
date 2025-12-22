package com.tissue.team.application.port.in;

import com.tissue.team.application.dto.request.CreateTeamCommand;
import com.tissue.team.application.dto.request.UpdateTeamCommand;
import com.tissue.team.application.dto.response.GetTeams;
import com.tissue.team.application.dto.response.TeamCreateResponse;
import com.tissue.team.application.dto.response.TeamDetail;

// TODO: add @PreAuthorize
public interface TeamUseCase {

	TeamCreateResponse create(CreateTeamCommand cmd);

	void update(UpdateTeamCommand cmd);

	void delete(String workspaceKey, Long teamId);

	TeamDetail getTeam(String workspaceKey, Long teamId);

	GetTeams getTeams(String workspaceKey);
}
