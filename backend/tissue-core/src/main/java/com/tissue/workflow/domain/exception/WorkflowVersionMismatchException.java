package com.tissue.workflow.domain.exception;

import com.tissue.common.exception.base.ResourceConflictException;

public class WorkflowVersionMismatchException extends ResourceConflictException {

    public WorkflowVersionMismatchException(Long clientVersion, Long currentVersion) {
        super(
                WorkflowErrorCode.WORKFLOW_VERSION_MISMATCH,
                "Workflow Client version: %d, Current version: %d".formatted(clientVersion, currentVersion));
        addContext("clientVersion", clientVersion);
        addContext("currentVersion", currentVersion);
    }
}
