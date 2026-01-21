package com.tissue.issuetype.domain.exception;

import static com.tissue.global.exception.ContextKeys.ISSUE_TYPE_ID;
import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ResourceNotFoundException;
import com.tissue.project.domain.Project;

public class IssueTypeNotFoundException extends ResourceNotFoundException {

    public IssueTypeNotFoundException(Long issueTypeId, Project project) {
        super(IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND);
        addContext(ISSUE_TYPE_ID, issueTypeId);
        addContext(PROJECT_KEY, project.getKey());
        addContext(WORKSPACE_KEY, project.getWorkspaceKey());
    }
}
