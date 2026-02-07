package com.tissue.sprint.domain.exception;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.sprint.domain.SprintStatus;

public class InvalidSprintStatusTransitionException extends BadRequestException {

    public InvalidSprintStatusTransitionException(
            SprintStatus currentStatus, SprintStatus requiredCurrentStatus, SprintStatus targetStatus) {
        super(SprintErrorCode.INVALID_SPRINT_STATUS_TRANSITION);
        addContext("currentStatus", currentStatus);
        addContext("requiredCurrentStatus", requiredCurrentStatus);
        addContext("targetStatus", targetStatus);
    }
}
