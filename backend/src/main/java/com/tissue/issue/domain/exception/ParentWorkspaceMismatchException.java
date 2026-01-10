package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.CHILD_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.CHILD_WORKSPACE_KEY;
import static com.tissue.global.exception.ContextKeys.PARENT_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.PARENT_WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;

public class ParentWorkspaceMismatchException extends BadRequestException {

    public ParentWorkspaceMismatchException(
            String parentWorkspaceKey, String parentIssueKey, String childWorkspaceKey, String childIssueKey) {
        super(IssueErrorCode.PARENT_WORKSPACE_MISMATCH);
        addContext(PARENT_WORKSPACE_KEY, parentWorkspaceKey);
        addContext(PARENT_ISSUE_KEY, parentIssueKey);
        addContext(CHILD_WORKSPACE_KEY, childWorkspaceKey);
        addContext(CHILD_ISSUE_KEY, childIssueKey);
    }
}
