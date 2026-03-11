package com.tissue.feature.project.application.service.finder;

import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectFinder {

    private final ProjectQueryRepository queryRepository;

    public Project getBy(String workspaceKey, String projectKey) {
        return queryRepository
                .findByWorkspaceKeyAndKey(workspaceKey, projectKey)
                .orElseThrow(() -> new ProjectNotFoundException(workspaceKey, projectKey));
    }

    public Project getWithWorkspaceBy(String workspaceKey, String projectKey) {
        return queryRepository
                .findWithWorkspaceByWorkspaceKeyAndProjectKey(workspaceKey, projectKey)
                .orElseThrow(() -> new ProjectNotFoundException(workspaceKey, projectKey));
    }

    public Project getWithLockBy(String workspaceKey, String projectKey) {
        return queryRepository
                .findByWorkspaceKeyAndProjectKeyWithLock(workspaceKey, projectKey)
                .orElseThrow(() -> new ProjectNotFoundException(workspaceKey, projectKey));
    }

    public Optional<Project> getOptionalBy(String workspaceKey, String projectKey) {
        return queryRepository.findByWorkspaceKeyAndKey(workspaceKey, projectKey);
    }

    public Project getDeletedBy(String workspaceKey, String projectKey) {
        return queryRepository
                .findDeletedByWorkspaceKeyAndKey(workspaceKey, projectKey)
                .orElseThrow(() -> new ProjectNotFoundException(workspaceKey, projectKey));
    }

    public int countByWorkspaceKey(String workspaceKey) {
        return queryRepository.countByWorkspaceKey(workspaceKey);
    }
}
