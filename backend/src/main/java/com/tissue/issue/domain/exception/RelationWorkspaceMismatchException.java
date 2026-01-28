package com.tissue.issue.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.SOURCE_ISSUE_KEY;
import static com.tissue.common.exception.ErrorContextKeys.SOURCE_WORKSPACE_KEY;
import static com.tissue.common.exception.ErrorContextKeys.TARGET_ISSUE_KEY;
import static com.tissue.common.exception.ErrorContextKeys.TARGET_WORKSPACE_KEY;

import com.tissue.common.exception.base.BadRequestException;

public class RelationWorkspaceMismatchException extends BadRequestException {

    public RelationWorkspaceMismatchException(
            String sourceWorkspaceKey, String sourceIssueKey, String targetWorkspaceKey, String targetIssueKey) {
        super(IssueErrorCode.RELATION_WORKSPACE_MISMATCH);
        addContext(SOURCE_WORKSPACE_KEY, sourceWorkspaceKey);
        addContext(SOURCE_ISSUE_KEY, sourceIssueKey);
        addContext(TARGET_WORKSPACE_KEY, targetWorkspaceKey);
        addContext(TARGET_ISSUE_KEY, targetIssueKey);
    }
}
