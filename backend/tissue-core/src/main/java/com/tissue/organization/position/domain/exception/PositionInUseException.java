package com.tissue.organization.position.domain.exception;

import static com.tissue.exception.ErrorContextKeys.POSITION_ID;
import static com.tissue.exception.ErrorContextKeys.POSITION_NAME;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.BadRequestException;
import com.tissue.organization.position.domain.Position;

public class PositionInUseException extends BadRequestException {

    public PositionInUseException(Position position) {
        super(PositionErrorCode.POSITION_IN_USE);
        addContext(WORKSPACE_KEY, position.getWorkspaceKey());
        addContext(POSITION_ID, position.getId());
        addContext(POSITION_NAME, position.getName().toString());
    }
}
