package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_TYPE;
import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;

import com.tissue.feature.project.domain.Project;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;

public class DuplicateIssueTypeNameException extends ResourceConflictException {

    public DuplicateIssueTypeNameException(Name name, Project project) {
        super(IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME);
        addContext(ISSUE_TYPE, name.getNormalized());
        addContext(PROJECT_KEY, project.getKey());
    }
}
