package com.tissue.organization.team.domain.exception;

import static com.tissue.exception.ErrorContextKeys.TEAM_ID;
import static com.tissue.exception.ErrorContextKeys.TEAM_NAME;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.BadRequestException;
import com.tissue.organization.team.domain.Team;

public class TeamInUseException extends BadRequestException {

    public TeamInUseException(Team team) {
        super(TeamErrorCode.TEAM_IN_USE);
        addContext(WORKSPACE_KEY, team.getWorkspaceKey());
        addContext(TEAM_ID, team.getId());
        addContext(TEAM_NAME, team.getName().toString());
    }
}
