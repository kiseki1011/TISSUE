package com.tissue.vcs.application.port.in;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.vcs.adapter.in.web.dto.response.VcsIntegrationDetail;

public interface WorkspaceVcsQueryUseCase {

    VcsIntegrationDetail getIntegration(String workspaceKey, ProjectMemberContext actorContext);
}
