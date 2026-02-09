package com.tissue.workflow.domain.exception;

import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.exception.base.ResourceNotFoundException;

public class WorkflowNotFoundException extends ResourceNotFoundException {

    public WorkflowNotFoundException(Long workflowId) {
        super(WorkflowErrorCode.WORKFLOW_NOT_FOUND);
        addContext(WORKFLOW_ID, workflowId);
    }

    public WorkflowNotFoundException(String projectKey, Long workflowId) {
        super(WorkflowErrorCode.WORKFLOW_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(WORKFLOW_ID, workflowId);
    }
}
