package com.tissue.feature.organization.team.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.TEAM_NAME;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceConflictException;

public class DuplicateTeamNameException extends ResourceConflictException {

    public DuplicateTeamNameException(String teamName, String workspaceKey) {
        super(TeamErrorCode.DUPLICATE_TEAM_NAME);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(TEAM_NAME, teamName);
    }
}
