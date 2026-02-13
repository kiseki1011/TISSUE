package com.tissue.feature.vcs.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.VCS_PROVIDER;
import static com.tissue.shared.exception.ErrorContextKeys.WORKSPACE_KEY;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class WorkspaceVcsIntegrationNotFoundException extends ResourceNotFoundException {

    public WorkspaceVcsIntegrationNotFoundException(String workspaceKey, String vcsProvider) {
        super(VcsErrorCode.INTEGRATION_NOT_FOUND);
        addContext(WORKSPACE_KEY, workspaceKey);
        addContext(VCS_PROVIDER, vcsProvider);
    }
}
