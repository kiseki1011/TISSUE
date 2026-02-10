package com.tissue.feature.workflow.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;

public class InitialStateCategoryMismatchException extends BadRequestException {

    public InitialStateCategoryMismatchException() {
        super(WorkflowErrorCode.INITIAL_STATE_CATEGORY_MISMATCH);
    }
}
