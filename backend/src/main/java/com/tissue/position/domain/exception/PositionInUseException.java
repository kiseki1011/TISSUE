package com.tissue.position.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.POSITION_ID;
import static com.tissue.common.exception.ErrorContextKeys.POSITION_NAME;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.position.domain.Position;

public class PositionInUseException extends BadRequestException {

    public PositionInUseException(Position position) {
        super(PositionErrorCode.POSITION_IN_USE);
        addContext(WORKSPACE_KEY, position.getWorkspaceKey());
        addContext(POSITION_ID, position.getId());
        addContext(POSITION_NAME, position.getName().toString());
    }
}
