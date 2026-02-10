package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_TYPE;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_TYPE_ID;

import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;

public class DuplicateIssueFieldNameException extends ResourceConflictException {

    public DuplicateIssueFieldNameException(Name name, IssueType issueType) {
        super(IssueTypeErrorCode.DUPLICATE_ISSUE_FIELD_NAME);
        addContext(ISSUE_FIELD, name.getNormalized());
        addContext(ISSUE_TYPE, issueType.getName());
        addContext(ISSUE_TYPE_ID, issueType.getId());
    }
}
