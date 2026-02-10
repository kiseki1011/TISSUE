package com.tissue.feature.project.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MEMBER_ID;
import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceConflictException;

public class ProjectMemberAlreadyExistsException extends ResourceConflictException {

    public ProjectMemberAlreadyExistsException(String workspaceKey, String projectKey, Long memberId) {
        super(ProjectErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext(MEMBER_ID, memberId);
    }
}
