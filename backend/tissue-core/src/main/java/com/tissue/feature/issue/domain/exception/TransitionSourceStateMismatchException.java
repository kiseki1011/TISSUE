package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.CURRENT_STATE;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.REQUIRED_STATE;
import static com.tissue.shared.exception.ErrorContextKeys.TRANSITION_ID;

import com.tissue.shared.exception.base.BadRequestException;

public class TransitionSourceStateMismatchException extends BadRequestException {

    public TransitionSourceStateMismatchException(
            String issueKey, Long transitionId, String currentState, String requiredState) {
        super(IssueErrorCode.TRANSITION_SOURCE_STATE_NOT_MATCH);
        addContext(ISSUE_KEY, issueKey);
        addContext(TRANSITION_ID, transitionId);
        addContext(CURRENT_STATE, currentState);
        addContext(REQUIRED_STATE, requiredState);
    }
}
