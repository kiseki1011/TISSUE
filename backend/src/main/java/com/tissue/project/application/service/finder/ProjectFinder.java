package com.tissue.project.application.service.finder;

import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.ProjectNotFoundException;
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

    public Optional<Project> getOptionalBy(String workspaceKey, String projectKey) {
        return queryRepository.findByWorkspaceKeyAndKey(workspaceKey, projectKey);
    }
}
