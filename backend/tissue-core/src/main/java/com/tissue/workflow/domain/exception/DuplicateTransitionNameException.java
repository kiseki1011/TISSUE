package com.tissue.workflow.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.TRANSITION;
import static com.tissue.common.exception.ErrorContextKeys.WORKFLOW;
import static com.tissue.common.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.common.exception.base.ResourceConflictException;

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
