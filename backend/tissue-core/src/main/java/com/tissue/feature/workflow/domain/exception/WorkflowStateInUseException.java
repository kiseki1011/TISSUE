package com.tissue.feature.workflow.domain.exception;

import com.tissue.shared.exception.base.BadRequestException;
import java.util.Collection;

public class WorkflowStateInUseException extends BadRequestException {

    public WorkflowStateInUseException(Collection<String> usedStateNames) {
        super(WorkflowErrorCode.WORKFLOW_STATE_IN_USE);
        addContext("usedStateNames", usedStateNames);
    }
}
