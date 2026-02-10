package com.tissue.feature.project.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.feature.project.domain.Project;
import com.tissue.shared.exception.base.BadRequestException;

public class ProjectArchivedException extends BadRequestException {

    public ProjectArchivedException(String workspaceKey, String projectKey) {
        super(ProjectErrorCode.PROJECT_ARCHIVED);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(PROJECT_KEY, projectKey);
    }

    public ProjectArchivedException(Project project) {
        super(ProjectErrorCode.PROJECT_ARCHIVED);
        addContext(WORKSPACE_KEY, project.getWorkspaceKey());
        addContext(PROJECT_KEY, project.getKey());
    }
}
