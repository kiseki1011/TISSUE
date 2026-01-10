package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.SOURCE_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.TARGET_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;

public class RelationAlreadyExistsException extends BadRequestException {

    public RelationAlreadyExistsException(String workspaceKey, String sourceIssueKey, String targetIssueKey) {
        super(IssueErrorCode.RELATION_ALREADY_EXISTS);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(SOURCE_ISSUE_KEY, sourceIssueKey);
        addContext(TARGET_ISSUE_KEY, targetIssueKey);
    }
}
