package com.tissue.workflow.domain.exception;

import static com.tissue.exception.ErrorContextKeys.TRANSITION;
import static com.tissue.exception.ErrorContextKeys.WORKFLOW;
import static com.tissue.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.exception.base.ResourceConflictException;

public class DuplicateTransitionNameException extends ResourceConflictException {

    public DuplicateTransitionNameException(
            String transitionName, String sourceStateName, String workflowName, Long workflowId) {
        super(WorkflowErrorCode.DUPLICATE_TRANSITION_NAME);
        addContext(TRANSITION, transitionName);
        addContext("sourceStateName", sourceStateName);
        addContext(WORKFLOW, workflowName);
        addContext(WORKFLOW_ID, workflowId);
    }
}
