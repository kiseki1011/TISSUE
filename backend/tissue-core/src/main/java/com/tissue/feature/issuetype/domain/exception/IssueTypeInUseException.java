package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_TYPE_ID;

import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.shared.exception.base.BadRequestException;

public class IssueTypeInUseException extends BadRequestException {

    public IssueTypeInUseException(IssueType issueType) {
        super(IssueTypeErrorCode.ISSUE_TYPE_IN_USE);
        addContext(ISSUE_TYPE_ID, issueType.getId());
    }
}
