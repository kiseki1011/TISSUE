package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.CHILD_HIERARCHY;
import static com.tissue.shared.exception.ErrorContextKeys.CHILD_ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.PARENT_HIERARCHY;
import static com.tissue.shared.exception.ErrorContextKeys.PARENT_ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.shared.exception.base.BadRequestException;

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
