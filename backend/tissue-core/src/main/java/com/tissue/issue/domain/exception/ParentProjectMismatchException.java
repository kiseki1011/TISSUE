package com.tissue.issue.domain.exception;

import static com.tissue.exception.ErrorContextKeys.CHILD_HIERARCHY;
import static com.tissue.exception.ErrorContextKeys.CHILD_ISSUE_KEY;
import static com.tissue.exception.ErrorContextKeys.PARENT_HIERARCHY;
import static com.tissue.exception.ErrorContextKeys.PARENT_ISSUE_KEY;

import com.tissue.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueHierarchy;

public class ParentProjectMismatchException extends BadRequestException {

    public ParentProjectMismatchException(
            IssueHierarchy parentHierarchy,
            String parentIssueKey,
            IssueHierarchy childHierarchy,
            String childIssueKey) {
        super(IssueErrorCode.PARENT_PROJECT_MISMATCH);
        addContext(PARENT_HIERARCHY, parentHierarchy);
        addContext(PARENT_ISSUE_KEY, parentIssueKey);
        addContext(CHILD_HIERARCHY, childHierarchy);
        addContext(CHILD_ISSUE_KEY, childIssueKey);
    }
}
