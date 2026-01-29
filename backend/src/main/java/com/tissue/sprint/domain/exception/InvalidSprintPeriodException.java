package com.tissue.sprint.domain.exception;

import com.tissue.common.exception.base.BadRequestException;
import java.time.Instant;

public class InvalidSprintPeriodException extends BadRequestException {

    public InvalidSprintPeriodException(Instant start, Instant end) {
        super(SprintErrorCode.INVALID_SPRINT_PERIOD);
        addContext("startDate", start);
        addContext("endDate", end);
    }
}
