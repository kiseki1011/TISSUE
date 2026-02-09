package com.tissue.workflow.domain.exception;

import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.exception.ErrorContextKeys.STATE_ID;
import static com.tissue.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.exception.base.ResourceNotFoundException;

public class WorkflowStateNotFoundException extends ResourceNotFoundException {

    public WorkflowStateNotFoundException(String projectKey, Long workflowId, Long stateId) {
        super(WorkflowErrorCode.WORKFLOW_STATE_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(WORKFLOW_ID, workflowId);
        addContext(STATE_ID, stateId);
    }
}
