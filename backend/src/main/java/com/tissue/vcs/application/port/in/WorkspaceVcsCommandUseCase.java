package com.tissue.vcs.application.port.in;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.vcs.adapter.in.web.dto.response.VcsSecretResponse;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.vcs.domain.enums.VcsProvider;

@Transactional
public interface WorkspaceVcsCommandUseCase {

    VcsSecretResponse regenerateSecret(String workspaceKey, VcsProvider provider, ProjectMemberContext actorContext);

    void removeIntegration(String workspaceKey, VcsProvider provider, ProjectMemberContext actorContext);
}
