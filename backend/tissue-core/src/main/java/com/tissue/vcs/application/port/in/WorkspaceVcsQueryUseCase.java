package com.tissue.vcs.application.port.in;

import com.tissue.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.vcs.domain.enums.VcsProvider;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public interface WorkspaceVcsQueryUseCase {

    VcsIntegrationDetail getIntegration(String workspaceKey, VcsProvider provider, WorkspaceMemberContext actorContext);
}
