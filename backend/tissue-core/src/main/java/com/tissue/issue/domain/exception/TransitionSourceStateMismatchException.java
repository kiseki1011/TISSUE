package com.tissue.issue.domain.exception;

import static com.tissue.exception.ErrorContextKeys.CURRENT_STATE;
import static com.tissue.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.exception.ErrorContextKeys.REQUIRED_STATE;
import static com.tissue.exception.ErrorContextKeys.TRANSITION_ID;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.BadRequestException;

public class TransitionSourceStateMismatchException extends BadRequestException {

    public TransitionSourceStateMismatchException(
            String workspaceKey, String issueKey, Long transitionId, String currentState, String requiredState) {
        super(IssueErrorCode.TRANSITION_SOURCE_STATE_NOT_MATCH);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(ISSUE_KEY, issueKey);
        addContext(TRANSITION_ID, transitionId);
        addContext(CURRENT_STATE, currentState);
        addContext(REQUIRED_STATE, requiredState);
    }
}
