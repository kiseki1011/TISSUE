package com.tissue.workflow.domain.exception;

import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKFLOW;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ResourceConflictException;

public class DuplicateWorkflowNameException extends ResourceConflictException {

    public DuplicateWorkflowNameException(String workflowName, String projectKey, String workspaceKey) {
        super(WorkflowErrorCode.DUPLICATE_WORKFLOW_NAME);
        addContext(WORKFLOW, workflowName);
        addContext(PROJECT_KEY, projectKey);
        addContext(WORKSPACE_KEY, workspaceKey);
    }
}
