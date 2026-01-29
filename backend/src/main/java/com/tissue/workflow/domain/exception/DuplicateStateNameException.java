package com.tissue.workflow.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.STATE;
import static com.tissue.common.exception.ErrorContextKeys.WORKFLOW;
import static com.tissue.common.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.common.exception.base.ResourceConflictException;

public class DuplicateStateNameException extends ResourceConflictException {

    public DuplicateStateNameException(String stateName, String workflowName, Long workflowId) {
        super(WorkflowErrorCode.DUPLICATE_STATE_NAME);
        addContext(STATE, stateName);
        addContext(WORKFLOW, workflowName);
        addContext(WORKFLOW_ID, workflowId);
    }
}
