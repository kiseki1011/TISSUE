package com.tissue.team.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.TEAM_ID;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class TeamNotFoundException extends ResourceNotFoundException {

    public TeamNotFoundException(Long teamId, String workspaceKey) {
        super(TeamErrorCode.TEAM_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(TEAM_ID, teamId);
    }
}
