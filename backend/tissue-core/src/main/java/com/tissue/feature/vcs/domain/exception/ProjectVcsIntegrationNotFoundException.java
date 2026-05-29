package com.tissue.feature.vcs.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.PROJECT_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.VCS_PROVIDER;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class ProjectVcsIntegrationNotFoundException extends ResourceNotFoundException {

    public ProjectVcsIntegrationNotFoundException(String projectKey, String vcsProvider) {
        super(VcsErrorCode.INTEGRATION_NOT_FOUND);
        addContext(PROJECT_KEY, projectKey);
        addContext(VCS_PROVIDER, vcsProvider);
    }
}
