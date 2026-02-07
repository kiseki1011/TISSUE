package com.tissue.workflow.domain.exception;

import com.tissue.common.exception.base.BadRequestException;
import java.util.Collection;

public class InvalidTransitionTargetException extends BadRequestException {

    public InvalidTransitionTargetException(Collection<String> sourceStateNames, String targetStateName) {
        super(WorkflowErrorCode.INVALID_TRANSITION_TARGET);
        addContext("invalidSourceStates", sourceStateNames);
        addContext("targetState", targetStateName);
    }
}
