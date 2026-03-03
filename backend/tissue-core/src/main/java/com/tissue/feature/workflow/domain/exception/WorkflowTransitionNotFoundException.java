package com.tissue.feature.workflow.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.TRANSITION_ID;
import static com.tissue.shared.exception.ErrorContextKeys.WORKFLOW_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;
import org.jspecify.annotations.Nullable;

public class WorkflowTransitionNotFoundException extends ResourceNotFoundException {

    public WorkflowTransitionNotFoundException(String projectKey, Long workflowId, @Nullable Long transitionId) {
        super(WorkflowErrorCode.WORKFLOW_TRANSITION_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(WORKFLOW_ID, workflowId);
        addContext(TRANSITION_ID, transitionId);
    }
}
