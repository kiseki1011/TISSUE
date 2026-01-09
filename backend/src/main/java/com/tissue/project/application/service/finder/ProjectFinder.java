package com.tissue.project.application.service.finder;

import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.ProjectArchivedException;
import com.tissue.project.domain.exception.ProjectNotFoundException;
import com.tissue.workspace.domain.exception.WorkspaceExceptions;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectFinder {

    private final ProjectQueryRepository queryRepository;

    // TODO: add javadoc for the following information
    //  - its only for read-only API's
    // TODO: should i change the name to getReadableBy or just use getBy?
    public Project getBy(String projectKey, String workspaceKey) {
        // TODO: use JOIN FETCH with Workspace at findByKeyAndWorkspace_Key for optimization
        // TODO: findByKeyAndWorkspaceKey vs findByKeyAndWorkspace_Key which is better?
        return queryRepository
                .findByKeyAndWorkspaceKey(projectKey, workspaceKey)
                .orElseThrow(() -> new ProjectNotFoundException(workspaceKey, projectKey));
    }

    // TODO: add javadoc for the following information
    //  - its only for command API's
    //  - will throw an exception if workspace or project was archived
    // TODO: 성능 최적화 할 방법이 있을까?
    public Project getModifiableBy(String projectKey, String workspaceKey) {
        Project project = getBy(projectKey, workspaceKey);

        if (project.getWorkspace().isArchived()) {
            throw WorkspaceExceptions.archived(project.getWorkspace());
        }
        if (project.isArchived()) {
            throw new ProjectArchivedException(workspaceKey, projectKey);
        }

        return project;
    }

    public Optional<Project> getOptionalBy(Long projectId) {
        return queryRepository.findById(projectId);
    }
}
