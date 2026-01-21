package com.tissue.issuetype.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_TYPE;
import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;

import com.tissue.common.vo.Name;
import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.project.domain.Project;

public class DuplicateIssueTypeNameException extends ResourceConflictException {

    public DuplicateIssueTypeNameException(Name name, Project project) {
        super(IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME);
        addContext(ISSUE_TYPE, name.getNormalized());
        addContext(PROJECT_KEY, project.getKey());
    }
}
