package com.tissue.feature.organization.team.application.port.usecase;

import com.tissue.feature.organization.team.application.dto.response.TeamSummary;
import java.util.List;

public interface TeamQueryUseCase {

    List<TeamSummary> getTeams(Long actorMemberId);
}
