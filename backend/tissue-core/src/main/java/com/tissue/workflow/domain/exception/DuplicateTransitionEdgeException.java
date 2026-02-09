package com.tissue.workflow.domain.exception;

import com.tissue.exception.base.BadRequestException;

public class DuplicateTransitionEdgeException extends BadRequestException {

    public DuplicateTransitionEdgeException(String sourceStateName, String targetStateName) {
        super(WorkflowErrorCode.DUPLICATE_TRANSITION_EDGE);
        addContext("sourceState", sourceStateName);
        addContext("targetState", targetStateName);
    }
}
