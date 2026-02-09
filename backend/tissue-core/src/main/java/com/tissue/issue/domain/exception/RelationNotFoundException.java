package com.tissue.issue.domain.exception;

import static com.tissue.exception.ErrorContextKeys.SOURCE_ISSUE_KEY;
import static com.tissue.exception.ErrorContextKeys.TARGET_ISSUE_KEY;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ResourceNotFoundException;

public class RelationNotFoundException extends ResourceNotFoundException {

    public RelationNotFoundException(String workspaceKey, String sourceIssueKey, String targetIssueKey) {
        super(IssueErrorCode.RELATION_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(SOURCE_ISSUE_KEY, sourceIssueKey);
        addContext(TARGET_ISSUE_KEY, targetIssueKey);
    }
}
