package com.tissue.feature.workflow.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKFLOW;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceConflictException;

public class DuplicateWorkflowNameException extends ResourceConflictException {

    public DuplicateWorkflowNameException(String workflowName, String projectKey, String workspaceKey) {
        super(WorkflowErrorCode.DUPLICATE_WORKFLOW_NAME);
        addContext(WORKFLOW, workflowName);
        addContext(PROJECT_KEY, projectKey);
        addContext(WORKSPACE_KEY, workspaceKey);
    }
}
