package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.RELATION_TYPE;
import static com.tissue.global.exception.ContextKeys.SOURCE_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.SOURCE_ISSUE_TYPE;
import static com.tissue.global.exception.ContextKeys.TARGET_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.TARGET_ISSUE_TYPE;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueRelationType;

public class RelationIssueTypeMismatchException extends BadRequestException {

    public RelationIssueTypeMismatchException(
            String workspaceKey,
            IssueRelationType relationType,
            String sourceIssueKey,
            String sourceIssueType,
            String targetIssueKey,
            String targetIssueType) {
        super(IssueErrorCode.RELATION_ISSUE_TYPE_MISMATCH);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(RELATION_TYPE, relationType);
        addContext(SOURCE_ISSUE_KEY, sourceIssueKey);
        addContext(SOURCE_ISSUE_TYPE, sourceIssueType);
        addContext(TARGET_ISSUE_KEY, targetIssueKey);
        addContext(TARGET_ISSUE_TYPE, targetIssueType);
    }
}
