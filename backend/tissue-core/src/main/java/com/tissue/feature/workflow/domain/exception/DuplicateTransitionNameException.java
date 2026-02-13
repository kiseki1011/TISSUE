package com.tissue.feature.workflow.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.TRANSITION;
import static com.tissue.shared.exception.ErrorContextKeys.WORKFLOW;

import com.tissue.shared.exception.base.ResourceConflictException;

public class DuplicateTransitionNameException extends ResourceConflictException {

    public DuplicateTransitionNameException(String transitionName, String sourceStateName, String workflowName) {
        super(WorkflowErrorCode.DUPLICATE_TRANSITION_NAME);
        addContext(TRANSITION, transitionName);
        addContext("sourceStateName", sourceStateName);
        addContext(WORKFLOW, workflowName);
    }
}
