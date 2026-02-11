package com.tissue.feature.vcs.application.port.usecase;

import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface WorkspaceVcsCommandUseCase {

    VcsSecretResponse regenerateSecret(String workspaceKey, VcsProvider provider, WorkspaceMemberContext actorContext);

    void removeIntegration(String workspaceKey, VcsProvider provider, WorkspaceMemberContext actorContext);
}
