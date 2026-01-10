package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.STATE;
import static com.tissue.global.exception.ContextKeys.WORKFLOW;
import static com.tissue.global.exception.ContextKeys.WORKFLOW_ID;

import com.tissue.global.exception.base.ResourceConflictException;

public class DuplicateStateNameException extends ResourceConflictException {

    public DuplicateStateNameException(String stateName, String workflowName, Long workflowId) {
        super(WorkflowErrorCode.DUPLICATE_STATE_NAME);
        addContext(STATE, stateName);
        addContext(WORKFLOW, workflowName);
        addContext(WORKFLOW_ID, workflowId);
    }
}
