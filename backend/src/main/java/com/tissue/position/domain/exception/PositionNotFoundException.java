package com.tissue.position.domain.exception;

import static com.tissue.global.exception.ContextKeys.POSITION_ID;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class PositionNotFoundException extends ResourceNotFoundException {

    public PositionNotFoundException(Long positionId, String workspaceKey) {
        super(PositionErrorCode.POSITION_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(POSITION_ID, positionId);
    }
}
