package com.tissue.feature.vcs.application.port.in;

import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;

public interface WorkspaceVcsQueryUseCase {

    VcsIntegrationDetail getIntegration(String workspaceKey, VcsProvider provider, WorkspaceMemberContext actorContext);
}
