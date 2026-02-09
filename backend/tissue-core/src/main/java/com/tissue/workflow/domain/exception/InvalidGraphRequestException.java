package com.tissue.workflow.domain.exception;

import com.tissue.exception.base.BadRequestException;

public class InvalidGraphRequestException extends BadRequestException {

    public InvalidGraphRequestException(String detail, String target, String errorType) {
        super(WorkflowErrorCode.INVALID_GRAPH_REQUEST, detail);
        addContext("target", target);
        addContext("errorType", errorType);
    }
}
