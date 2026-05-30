package com.tissue.feature.project.application.service.finder;

import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectFinder {

    private final ProjectQueryRepository queryRepository;

    // projectKey is globally unique.
    public Project getByProjectKey(String projectKey) {
        return queryRepository.findByKey(projectKey).orElseThrow(() -> new ProjectNotFoundException(projectKey));
    }

    public Project getWithLockByProjectKey(String projectKey) {
        return queryRepository
                .findByProjectKeyWithLock(projectKey)
                .orElseThrow(() -> new ProjectNotFoundException(projectKey));
    }

    public Project getDeletedByProjectKey(String projectKey) {
        return queryRepository.findDeletedByKey(projectKey).orElseThrow(() -> new ProjectNotFoundException(projectKey));
    }
}
