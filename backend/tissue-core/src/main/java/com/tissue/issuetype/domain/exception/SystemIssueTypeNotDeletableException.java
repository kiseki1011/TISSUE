package com.tissue.issuetype.domain.exception;

import static com.tissue.exception.ErrorContextKeys.ISSUE_TYPE_ID;

import com.tissue.exception.base.BadRequestException;
import com.tissue.issuetype.domain.IssueType;

public class SystemIssueTypeNotDeletableException extends BadRequestException {

    public SystemIssueTypeNotDeletableException(IssueType issueType) {
        super(IssueTypeErrorCode.SYSTEM_ISSUE_TYPE_NOT_DELETABLE);
        addContext(ISSUE_TYPE_ID, issueType.getId());
    }
}
