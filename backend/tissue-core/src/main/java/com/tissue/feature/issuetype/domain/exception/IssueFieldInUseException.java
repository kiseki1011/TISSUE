package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_ID;

import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.shared.exception.base.BadRequestException;

public class IssueFieldInUseException extends BadRequestException {

    public IssueFieldInUseException(IssueField issueField) {
        super(IssueTypeErrorCode.ISSUE_FIELD_IN_USE);
        addContext(ISSUE_FIELD_ID, issueField.getId());
    }
}
