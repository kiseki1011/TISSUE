package com.tissue.issuetype.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.ISSUE_FIELD_ID;
import static com.tissue.common.exception.ErrorContextKeys.ISSUE_FIELD_OPTION;

import com.tissue.common.exception.base.ResourceConflictException;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.domain.IssueField;

public class DuplicateEnumFieldOptionNameException extends ResourceConflictException {

    public DuplicateEnumFieldOptionNameException(Name name, IssueField issueField) {
        super(IssueTypeErrorCode.DUPLICATE_FIELD_OPTION_NAME);
        addContext(ISSUE_FIELD_OPTION, name);
        addContext(ISSUE_FIELD_ID, issueField.getId());
    }
}
