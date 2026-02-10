package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.CURRENT_STATE;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.STATE_CATEGORY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.exception.base.BadRequestException;

public class OnlyInitialStateDeletionAllowedException extends BadRequestException {

    public OnlyInitialStateDeletionAllowedException(
            String workspaceKey, String issueKey, String currentState, StateCategory stateCategory) {
        super(IssueErrorCode.ONLY_INITIAL_STATE_DELETION_ALLOWED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(ISSUE_KEY, issueKey);
        addContext(CURRENT_STATE, currentState);
        addContext(STATE_CATEGORY, stateCategory);
    }
}
