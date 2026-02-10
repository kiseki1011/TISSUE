package com.tissue.feature.organization.position.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.POSITION_NAME;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceConflictException;

public class DuplicatePositionNameException extends ResourceConflictException {

    public DuplicatePositionNameException(String positionName, String workspaceKey) {
        super(PositionErrorCode.DUPLICATE_POSITION_NAME);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(POSITION_NAME, positionName);
    }
}
