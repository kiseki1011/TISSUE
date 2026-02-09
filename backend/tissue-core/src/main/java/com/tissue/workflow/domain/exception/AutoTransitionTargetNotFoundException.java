package com.tissue.workflow.domain.exception;

import static com.tissue.exception.ErrorContextKeys.CURRENT_STATE;
import static com.tissue.exception.ErrorContextKeys.ISSUE_KEY;

import com.tissue.exception.base.InternalServerException;

public class AutoTransitionTargetNotFoundException extends InternalServerException {

    public AutoTransitionTargetNotFoundException(
            String issueKey, String currentStateName, String targetTransitionName) {
        super(WorkflowErrorCode.AUTO_TRANSITION_TARGET_NOT_FOUND);
        addContext(ISSUE_KEY, issueKey);
        addContext(CURRENT_STATE, currentStateName);
        addContext("targetTransition", targetTransitionName);
    }
}
