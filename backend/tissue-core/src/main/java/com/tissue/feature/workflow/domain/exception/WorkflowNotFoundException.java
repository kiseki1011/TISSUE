package com.tissue.feature.workflow.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WorkflowNotFoundException extends ResourceNotFoundException {

    public WorkflowNotFoundException(Long workflowId) {
        super(WorkflowErrorCode.WORKFLOW_NOT_FOUND);
        addContext(WORKFLOW_ID, workflowId);
    }
}
