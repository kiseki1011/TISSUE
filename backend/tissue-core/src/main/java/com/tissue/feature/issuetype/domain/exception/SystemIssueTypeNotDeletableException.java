package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_TYPE_ID;

import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.shared.exception.base.BadRequestException;

public class SystemIssueTypeNotDeletableException extends BadRequestException {

    public SystemIssueTypeNotDeletableException(IssueType issueType) {
        super(IssueTypeErrorCode.SYSTEM_ISSUE_TYPE_NOT_DELETABLE);
        addContext(ISSUE_TYPE_ID, issueType.getId());
    }
}
