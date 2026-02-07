package com.tissue.issuetype.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.ISSUE_TYPE;
import static com.tissue.common.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.common.exception.base.ResourceConflictException;
import com.tissue.global.vo.Name;
import com.tissue.project.domain.Project;

public class DuplicateIssueTypeNameException extends ResourceConflictException {

    public DuplicateIssueTypeNameException(Name name, Project project) {
        super(IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME);
        addContext(ISSUE_TYPE, name.getNormalized());
        addContext(PROJECT_KEY, project.getKey());
    }
}
