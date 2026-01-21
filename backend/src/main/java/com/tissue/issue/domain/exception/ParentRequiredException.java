package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.CURRENT_HIERARCHY;
import static com.tissue.global.exception.ContextKeys.HIERARCHIES_REQUIRING_PARENT;
import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueHierarchy;

public class ParentRequiredException extends BadRequestException {

    public ParentRequiredException(String workspaceKey, String issueKey, IssueHierarchy currentHierarchy) {
        super(IssueErrorCode.PARENT_REQUIRED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(ISSUE_KEY, issueKey);
        addContext(CURRENT_HIERARCHY, currentHierarchy);
        addContext(HIERARCHIES_REQUIRING_PARENT, IssueHierarchy.getParentRequired());
    }
}
