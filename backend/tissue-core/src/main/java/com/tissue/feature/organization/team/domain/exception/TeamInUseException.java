package com.tissue.feature.organization.team.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.TEAM_ID;
import static com.tissue.shared.exception.ErrorContextKeys.TEAM_NAME;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.organization.team.domain.Team;
import com.tissue.shared.exception.base.BadRequestException;

public class TeamInUseException extends BadRequestException {

    public TeamInUseException(Team team) {
        super(TeamErrorCode.TEAM_IN_USE);
        addContext(WORKSPACE_KEY, team.getWorkspaceKey());
        addContext(TEAM_ID, team.getId());
        addContext(TEAM_NAME, team.getName().toString());
    }
}
