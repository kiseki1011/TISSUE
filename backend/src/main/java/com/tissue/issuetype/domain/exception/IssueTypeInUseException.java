package com.tissue.issuetype.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_TYPE_ID;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.issuetype.domain.IssueType;

public class IssueTypeInUseException extends BadRequestException {

    public IssueTypeInUseException(IssueType issueType) {
        super(IssueTypeErrorCode.ISSUE_TYPE_IN_USE);
        addContext(ISSUE_TYPE_ID, issueType.getId());
    }
}
