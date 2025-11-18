package com.tissue.api.project.domain.port.out;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.api.project.domain.Project;
import com.tissue.api.workspace.domain.model.Workspace;

public interface ProjectQueryRepository extends Repository<Project, Long> {

	Optional<Project> findByWorkspaceKeyAndProjectKey(String workspaceKey, String projectKey);

	Optional<Project> findByWorkspaceAndProjectKey(Workspace workspace, String projectKey);
}
