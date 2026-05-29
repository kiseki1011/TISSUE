package com.tissue.feature.vcs.application.port.repository;

import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface ProjectVcsIntegrationRepository extends Repository<ProjectVcsIntegration, Long> {

    ProjectVcsIntegration save(ProjectVcsIntegration vcsIntegration);

    void delete(ProjectVcsIntegration vcsIntegration);

    Optional<ProjectVcsIntegration> findByProjectKeyAndProvider(String projectKey, VcsProvider provider);
}
