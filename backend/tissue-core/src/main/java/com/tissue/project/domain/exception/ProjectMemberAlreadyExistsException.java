package com.tissue.project.domain.exception;

import static com.tissue.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.exception.base.ResourceConflictException;

public class ProjectMemberAlreadyExistsException extends ResourceConflictException {

    public ProjectMemberAlreadyExistsException(String workspaceKey, String projectKey, Long memberId) {
        super(ProjectErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext(MEMBER_ID, memberId);
    }
}
