package com.tissue.sprint.domain.exception;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.sprint.domain.enums.SprintStatus;

public class InvalidSprintStatusTransitionException extends BadRequestException {

    public InvalidSprintStatusTransitionException(
            SprintStatus currentStatus, SprintStatus requiredCurrentStatus, SprintStatus targetStatus) {
        super(SprintErrorCode.INVALID_SPRINT_STATUS_TRANSITION);
        addContext("currentStatus", currentStatus);
        addContext("requiredCurrentStatus", requiredCurrentStatus);
        addContext("targetStatus", targetStatus);
    }
}
