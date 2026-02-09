package com.tissue.project.domain.exception;

import static com.tissue.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ResourceNotFoundException;

public class ProjectMemberNotFoundException extends ResourceNotFoundException {

    public ProjectMemberNotFoundException(String workspaceKey, String projectKey, Long memberId) {
        super(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext(MEMBER_ID, memberId);
    }
}
