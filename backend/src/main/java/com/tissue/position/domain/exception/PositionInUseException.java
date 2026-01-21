package com.tissue.position.domain.exception;

import static com.tissue.global.exception.ContextKeys.POSITION_ID;
import static com.tissue.global.exception.ContextKeys.POSITION_NAME;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.position.domain.Position;

public class PositionInUseException extends BadRequestException {

    public PositionInUseException(Position position) {
        super(PositionErrorCode.POSITION_IN_USE);
        addContext(WORKSPACE_KEY, position.getWorkspaceKey());
        addContext(POSITION_ID, position.getId());
        addContext(POSITION_NAME, position.getDisplayName());
    }
}
