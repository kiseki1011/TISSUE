package com.tissue.feature.organization.team.application.port.usecase;

import com.tissue.feature.organization.team.application.dto.request.CreateTeamCommand;
import com.tissue.feature.organization.team.application.dto.request.PatchTeamCommand;
import com.tissue.feature.organization.team.application.dto.response.TeamResponse;

public interface TeamUseCase {

    TeamResponse create(CreateTeamCommand cmd, Long actorMemberId);

    void update(Long teamId, PatchTeamCommand cmd, Long actorMemberId);

    void delete(Long teamId, Long actorMemberId);
}
