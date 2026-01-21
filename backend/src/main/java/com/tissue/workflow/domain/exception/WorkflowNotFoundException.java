package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKFLOW_ID;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class WorkflowNotFoundException extends ResourceNotFoundException {

    public WorkflowNotFoundException(Long workflowId) {
        super(WorkflowErrorCode.WORKFLOW_NOT_FOUND);
        addContext(WORKFLOW_ID, workflowId);
    }

    public WorkflowNotFoundException(Long workflowId, String projectKey) {
        super(WorkflowErrorCode.WORKFLOW_NOT_FOUND);
        addContext(WORKFLOW_ID, workflowId);
        addContext(PROJECT_KEY, projectKey);
    }
}
