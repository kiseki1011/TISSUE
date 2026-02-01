package com.tissue.issuetype.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.ISSUE_FIELD;
import static com.tissue.common.exception.ErrorContextKeys.ISSUE_TYPE;
import static com.tissue.common.exception.ErrorContextKeys.ISSUE_TYPE_ID;

import com.tissue.common.exception.base.ResourceConflictException;
import com.tissue.global.vo.Name;
import com.tissue.issuetype.domain.IssueType;

public class DuplicateIssueFieldNameException extends ResourceConflictException {

    public DuplicateIssueFieldNameException(Name name, IssueType issueType) {
        super(IssueTypeErrorCode.DUPLICATE_ISSUE_FIELD_NAME);
        addContext(ISSUE_FIELD, name.getNormalized());
        addContext(ISSUE_TYPE, issueType.getDisplayName());
        addContext(ISSUE_TYPE_ID, issueType.getId());
    }
}
