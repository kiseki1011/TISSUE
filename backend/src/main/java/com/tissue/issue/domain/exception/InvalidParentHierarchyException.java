package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.CHILD_HIERARCHY;
import static com.tissue.global.exception.ContextKeys.CHILD_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.PARENT_HIERARCHY;
import static com.tissue.global.exception.ContextKeys.PARENT_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueHierarchy;

public class InvalidParentHierarchyException extends BadRequestException {

    public InvalidParentHierarchyException(
            String workspaceKey,
            String parentIssueKey,
            IssueHierarchy parentHierarchy,
            String childIssueKey,
            IssueHierarchy childHierarchy) {
        super(IssueErrorCode.INVALID_PARENT_HIERARCHY);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PARENT_ISSUE_KEY, parentIssueKey);
        addContext(PARENT_HIERARCHY, parentHierarchy);
        addContext(CHILD_ISSUE_KEY, childIssueKey);
        addContext(CHILD_HIERARCHY, childHierarchy);
    }
}
