package com.tissue.issuetype.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issuetype.domain.IssueField;

public class IssueFieldInUseException extends BadRequestException {

    public IssueFieldInUseException(IssueField issueField) {
        super(IssueTypeErrorCode.ISSUE_FIELD_IN_USE);
        addContext(ISSUE_FIELD_ID, issueField.getId());
    }
}
