package com.tissue.feature.workflow.domain.exception;

import com.tissue.shared.exception.base.ResourceConflictException;
import java.util.Collection;

public class WorkflowStateInUseException extends ResourceConflictException {

    public WorkflowStateInUseException(Collection<String> usedStateNames) {
        super(WorkflowErrorCode.WORKFLOW_STATE_IN_USE);
        addContext("usedStateNames", usedStateNames);
    }
}
