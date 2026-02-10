package com.tissue.feature.vcs.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WorkspaceVcsIntegrationNotFoundException extends ResourceNotFoundException {

    public WorkspaceVcsIntegrationNotFoundException(String workspaceKey) {
        super(VcsErrorCode.INTEGRATION_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
    }
}
