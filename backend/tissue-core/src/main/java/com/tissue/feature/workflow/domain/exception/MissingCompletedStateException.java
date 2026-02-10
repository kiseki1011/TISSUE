package com.tissue.feature.workflow.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;

public class MissingCompletedStateException extends BadRequestException {

    public MissingCompletedStateException() {
        super(WorkflowErrorCode.MISSING_COMPLETED_STATE);
    }
}
