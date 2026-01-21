package com.tissue.project.domain.exception;

import static com.tissue.global.exception.ContextKeys.MEMBER_ID;
import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ResourceNotFoundException;

public class ProjectMemberNotFoundException extends ResourceNotFoundException {

    public ProjectMemberNotFoundException(String workspaceKey, String projectKey, Long memberId) {
        super(ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext(MEMBER_ID, memberId);
    }
}
