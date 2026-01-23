package com.tissue.vcs.application.port.in;

import com.tissue.vcs.adapter.in.web.dto.response.VcsSecretResponse;
import com.tissue.vcs.domain.enums.VcsProvider;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface WorkspaceVcsCommandUseCase {

    VcsSecretResponse regenerateSecret(String workspaceKey, VcsProvider provider, WorkspaceMemberContext actorContext);

    void removeIntegration(String workspaceKey, VcsProvider provider, WorkspaceMemberContext actorContext);
}
