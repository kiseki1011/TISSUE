package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.TRANSITION;
import static com.tissue.global.exception.ContextKeys.WORKFLOW;
import static com.tissue.global.exception.ContextKeys.WORKFLOW_ID;

import com.tissue.global.exception.base.ResourceConflictException;

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
