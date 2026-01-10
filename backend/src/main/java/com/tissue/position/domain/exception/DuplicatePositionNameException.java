package com.tissue.position.domain.exception;

import static com.tissue.global.exception.ContextKeys.POSITION_NAME;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ResourceConflictException;

public class DuplicatePositionNameException extends ResourceConflictException {

    public DuplicatePositionNameException(String positionName, String workspaceKey) {
        super(PositionErrorCode.DUPLICATE_POSITION_NAME);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(POSITION_NAME, positionName);
    }
}
