package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_ID;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_OPTION;

import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;

public class DuplicateEnumFieldOptionNameException extends ResourceConflictException {

    public DuplicateEnumFieldOptionNameException(Name name, IssueField issueField) {
        super(IssueTypeErrorCode.DUPLICATE_FIELD_OPTION_NAME);
        addContext(ISSUE_FIELD_OPTION, name);
        addContext(ISSUE_FIELD_ID, issueField.getId());
    }
}
