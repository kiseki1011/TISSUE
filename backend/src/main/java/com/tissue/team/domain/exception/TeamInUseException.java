package com.tissue.team.domain.exception;

import static com.tissue.global.exception.ContextKeys.TEAM_ID;
import static com.tissue.global.exception.ContextKeys.TEAM_NAME;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.team.domain.Team;

public class TeamInUseException extends BadRequestException {

    public TeamInUseException(Team team) {
        super(TeamErrorCode.TEAM_IN_USE);
        addContext(WORKSPACE_KEY, team.getWorkspaceKey());
        addContext(TEAM_ID, team.getId());
        addContext(TEAM_NAME, team.getDisplayName());
    }
}
