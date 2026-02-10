package com.tissue.feature.issue.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.RELATION_TYPE;
import static com.tissue.shared.exception.ErrorContextKeys.SOURCE_ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.SOURCE_ISSUE_TYPE;
import static com.tissue.shared.exception.ErrorContextKeys.TARGET_ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.TARGET_ISSUE_TYPE;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.shared.exception.base.BadRequestException;

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
