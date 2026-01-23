package com.tissue.vcs.application.port.out;

import com.tissue.vcs.domain.WorkspaceVcsIntegration;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WorkspaceVcsIntegrationRepository extends Repository<WorkspaceVcsIntegration, Long> {

    WorkspaceVcsIntegration save(WorkspaceVcsIntegration vcsIntegration);

    Optional<WorkspaceVcsIntegration> findByWorkspaceKey(String workspaceKey);
}
