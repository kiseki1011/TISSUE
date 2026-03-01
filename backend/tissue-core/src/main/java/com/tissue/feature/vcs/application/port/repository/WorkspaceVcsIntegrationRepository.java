package com.tissue.feature.vcs.application.port.repository;

import com.tissue.feature.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WorkspaceVcsIntegrationRepository extends Repository<WorkspaceVcsIntegration, Long> {

    WorkspaceVcsIntegration save(WorkspaceVcsIntegration vcsIntegration);

    void delete(WorkspaceVcsIntegration vcsIntegration);

    Optional<WorkspaceVcsIntegration> findByWorkspaceKeyAndProvider(String workspaceKey, VcsProvider provider);
}
