package com.tissue.feature.workflow.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.STATE_ID;
import static com.tissue.shared.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WorkflowStateNotFoundException extends ResourceNotFoundException {

    public WorkflowStateNotFoundException(Long workflowId, Long stateId) {
        super(WorkflowErrorCode.WORKFLOW_STATE_NOT_FOUND);
        addContext(WORKFLOW_ID, workflowId);
        addContext(STATE_ID, stateId);
    }
}
