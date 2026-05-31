package com.tissue.feature.organization.position.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.POSITION_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class PositionNotFoundException extends ResourceNotFoundException {

    public PositionNotFoundException(Long positionId) {
        super(PositionErrorCode.POSITION_NOT_FOUND);
        addContext(POSITION_ID, positionId);
    }
}
