package com.tissue.project.domain.exception;

import static com.tissue.global.exception.ContextKeys.PROJECT_KEY;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;

import com.tissue.global.exception.base.ForbiddenException;

// TODO: 그냥 SprintOwnershipRequired, IssueOwnershipRequired, 등...으로 쪼개서 예외를 만들까?
public class ResourceOwnershipRequiredException extends ForbiddenException {

    public ResourceOwnershipRequiredException(String workspaceKey, String projectKey, String resourceType) {
        super(ProjectErrorCode.RESOURCE_OWNERSHIP_REQUIRED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext("resourceType", resourceType);
    }
}
