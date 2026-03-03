package com.tissue.feature.vcs.application.port.usecase;

import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.domain.enums.VcsProvider;

public interface WorkspaceVcsQueryUseCase {

    VcsIntegrationDetail getIntegration(String workspaceKey, VcsProvider provider, Long actorMemberId);
}
