package com.tissue.feature.project.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ForbiddenException;

// TODO: Should I just separate this into SprintOwnershipRequired, IssueOwnershipRequired, etc...?
public class ResourceOwnershipRequiredException extends ForbiddenException {

    public ResourceOwnershipRequiredException(String workspaceKey, String projectKey, String resourceType) {
        super(ProjectErrorCode.RESOURCE_OWNERSHIP_REQUIRED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
        addContext("resourceType", resourceType);
    }
}
