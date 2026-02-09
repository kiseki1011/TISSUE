package com.tissue.organization.team.domain.exception;

import static com.tissue.exception.ErrorContextKeys.TEAM_ID;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ResourceNotFoundException;

public class TeamNotFoundException extends ResourceNotFoundException {

    public TeamNotFoundException(String workspaceKey, Long teamId) {
        super(TeamErrorCode.TEAM_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(TEAM_ID, teamId);
    }
}
