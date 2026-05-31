package com.tissue.feature.organization.team.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.TEAM_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class TeamNotFoundException extends ResourceNotFoundException {

    public TeamNotFoundException(Long teamId) {
        super(TeamErrorCode.TEAM_NOT_FOUND);
        addContext(TEAM_ID, teamId);
    }
}
