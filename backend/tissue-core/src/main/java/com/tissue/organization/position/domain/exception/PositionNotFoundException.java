package com.tissue.organization.position.domain.exception;

import static com.tissue.exception.ErrorContextKeys.POSITION_ID;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ResourceNotFoundException;

public class PositionNotFoundException extends ResourceNotFoundException {

    public PositionNotFoundException(String workspaceKey, Long positionId) {
        super(PositionErrorCode.POSITION_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(POSITION_ID, positionId);
    }
}
