package com.tissue.feature.sprint.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;
import java.time.Instant;

public class InvalidSprintPeriodException extends BadRequestException {

    public InvalidSprintPeriodException(Instant start, Instant end) {
        super(SprintErrorCode.INVALID_SPRINT_PERIOD);
        addContext("startDate", start);
        addContext("endDate", end);
    }
}
