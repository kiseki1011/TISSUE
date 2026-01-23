package com.tissue.vcs.application.port.in;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.vcs.adapter.in.web.dto.response.VcsIntegrationDetail;

import com.tissue.vcs.domain.enums.VcsProvider;

public interface WorkspaceVcsQueryUseCase {

    VcsIntegrationDetail getIntegration(String workspaceKey, VcsProvider provider, ProjectMemberContext actorContext);
}
