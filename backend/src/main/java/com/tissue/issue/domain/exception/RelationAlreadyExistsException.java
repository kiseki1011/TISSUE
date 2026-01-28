package com.tissue.issue.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.SOURCE_ISSUE_KEY;
import static com.tissue.common.exception.ErrorContextKeys.TARGET_ISSUE_KEY;
import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.BadRequestException;

public class RelationAlreadyExistsException extends BadRequestException {

    public RelationAlreadyExistsException(String workspaceKey, String sourceIssueKey, String targetIssueKey) {
        super(IssueErrorCode.RELATION_ALREADY_EXISTS);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(SOURCE_ISSUE_KEY, sourceIssueKey);
        addContext(TARGET_ISSUE_KEY, targetIssueKey);
    }
}
