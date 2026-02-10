package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.CURRENT_HIERARCHY;
import static com.tissue.shared.exception.ErrorContextKeys.HIERARCHIES_REQUIRING_PARENT;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.shared.exception.base.BadRequestException;

public class ParentRequiredException extends BadRequestException {

    public ParentRequiredException(String workspaceKey, String issueKey, IssueHierarchy currentHierarchy) {
        super(IssueErrorCode.PARENT_REQUIRED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(ISSUE_KEY, issueKey);
        addContext(CURRENT_HIERARCHY, currentHierarchy);
        addContext(HIERARCHIES_REQUIRING_PARENT, IssueHierarchy.getParentRequired());
    }
}
