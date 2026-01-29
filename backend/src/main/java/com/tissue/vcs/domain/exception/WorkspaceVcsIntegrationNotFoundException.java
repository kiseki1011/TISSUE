package com.tissue.vcs.domain.exception;

import static com.tissue.common.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class WorkspaceVcsIntegrationNotFoundException extends ResourceNotFoundException {

    public WorkspaceVcsIntegrationNotFoundException(String workspaceKey) {
        super(VcsErrorCode.INTEGRATION_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
    }
}
