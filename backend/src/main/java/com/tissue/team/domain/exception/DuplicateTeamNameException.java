package com.tissue.team.domain.exception;

import static com.tissue.global.exception.ContextKeys.TEAM_NAME;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ResourceConflictException;

public class DuplicateTeamNameException extends ResourceConflictException {

    public DuplicateTeamNameException(String teamName, String workspaceKey) {
        super(TeamErrorCode.DUPLICATE_TEAM_NAME);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(TEAM_NAME, teamName);
    }
}
