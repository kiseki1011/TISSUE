package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.CURRENT_STATE;
import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.STATE_CATEGORY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.workflow.domain.enums.StateCategory;

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
