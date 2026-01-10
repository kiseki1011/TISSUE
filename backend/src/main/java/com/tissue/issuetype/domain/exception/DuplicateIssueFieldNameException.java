package com.tissue.issuetype.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_FIELD;
import static com.tissue.global.exception.ContextKeys.ISSUE_TYPE;
import static com.tissue.global.exception.ContextKeys.ISSUE_TYPE_ID;

import com.tissue.common.vo.Name;
import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.issuetype.domain.IssueType;

public class DuplicateIssueFieldNameException extends ResourceConflictException {

    public DuplicateIssueFieldNameException(Name name, IssueType issueType) {
        super(IssueTypeErrorCode.DUPLICATE_ISSUE_FIELD_NAME);
        addContext(ISSUE_FIELD, name.getNormalized());
        addContext(ISSUE_TYPE, issueType.getDisplayName());
        addContext(ISSUE_TYPE_ID, issueType.getId());
    }
}
