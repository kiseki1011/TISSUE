package com.tissue.feature.issuetype.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_TYPE_ID;
import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.project.domain.Project;
import com.tissue.shared.exception.base.ResourceNotFoundException;

public class IssueTypeNotFoundException extends ResourceNotFoundException {

    public IssueTypeNotFoundException(Long issueTypeId, Project project) {
        super(IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND);
        addContext(ISSUE_TYPE_ID, issueTypeId);
        addContext(PROJECT_KEY, project.getKey());
        addContext(WORKSPACE_KEY, project.getWorkspaceKey());
    }

    public IssueTypeNotFoundException(String projectKey, Long issueTypeId) {
        super(IssueTypeErrorCode.ISSUE_TYPE_NOT_FOUND);
        addContext(ISSUE_TYPE_ID, issueTypeId);
        addContext(PROJECT_KEY, projectKey);
    }
}
