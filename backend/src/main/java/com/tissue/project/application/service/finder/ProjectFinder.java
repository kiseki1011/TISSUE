package com.tissue.project.application.service.finder;

import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.ProjectArchivedException;
import com.tissue.project.domain.exception.ProjectNotFoundException;
import com.tissue.workspace.domain.exception.WorkspaceArchivedException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectFinder {

    private final ProjectQueryRepository queryRepository;

    /**
     * Retrieves a Project entity for read-only purposes.
     *
     * <p>This method does NOT check if the project or its workspace is archived.
     * It should only be used for query/read-only APIs where the archived status might not matter
     * or is handled separately.
     */
    public Project getBy(Long projectId) {
        return queryRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    /**
     * Retrieves a Project entity for command/modification purposes.
     *
     * <p>This method validates that the Project and its Workspace are NOT archived.
     * If either is archived, an exception is thrown to prevent modification.
     */
    public Project getModifiableBy(String projectKey, String workspaceKey) {
        Project project = queryRepository
                .findWithWorkspaceByKeyAndWorkspaceKey(projectKey, workspaceKey)
                .orElseThrow(() -> new ProjectNotFoundException(workspaceKey, projectKey));

        if (project.getWorkspace().isArchived()) {
            throw new WorkspaceArchivedException(project.getWorkspace());
        }
        if (project.isArchived()) {
            throw new ProjectArchivedException(project);
        }

        return project;
    }

    public Project getModifiableBy(Long projectId) {
        Project project =
                queryRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (project.getWorkspace().isArchived()) {
            throw new WorkspaceArchivedException(project.getWorkspace());
        }
        if (project.isArchived()) {
            throw new ProjectArchivedException(project);
        }

        return project;
    }

    public Optional<Project> getOptionalBy(Long projectId) {
        return queryRepository.findById(projectId);
    }

    public Optional<Project> getOptionalBy(String projectKey, String workspaceKey) {
        return queryRepository.findByKeyAndWorkspaceKey(projectKey, workspaceKey);
    }
}
