package com.tissue.issue.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.INPUT_DATE;

import com.tissue.common.exception.base.BadRequestException;
import java.time.Instant;

public class DueDateMustBeFutureException extends BadRequestException {

    public DueDateMustBeFutureException(Instant inputDate) {
        super(IssueErrorCode.DUE_DATE_MUST_BE_FUTURE);
        addContext(INPUT_DATE, inputDate);
    }
}
