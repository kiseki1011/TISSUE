package com.tissue.feature.sprint.domain.exception;

import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.shared.exception.base.BadRequestException;

public class InvalidSprintStatusTransitionException extends BadRequestException {

    public InvalidSprintStatusTransitionException(
            SprintStatus currentStatus, SprintStatus requiredCurrentStatus, SprintStatus targetStatus) {
        super(SprintErrorCode.INVALID_SPRINT_STATUS_TRANSITION);
        addContext("currentStatus", currentStatus);
        addContext("requiredCurrentStatus", requiredCurrentStatus);
        addContext("targetStatus", targetStatus);
    }
}
