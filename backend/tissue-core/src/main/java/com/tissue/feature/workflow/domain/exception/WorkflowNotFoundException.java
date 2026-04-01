package com.tissue.feature.workflow.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WorkflowNotFoundException extends ResourceNotFoundException {

    public WorkflowNotFoundException(String projectKey, Long workflowId) {
        super(WorkflowErrorCode.WORKFLOW_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(WORKFLOW_ID, workflowId);
    }

    public WorkflowNotFoundException(Long workflowId) {
        super(WorkflowErrorCode.WORKFLOW_NOT_FOUND);
        addContext(WORKFLOW_ID, workflowId);
    }
}
