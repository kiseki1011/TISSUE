package com.tissue.api.project.application.port.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.enums.ProjectVisibility;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.persistence.LockModeType;

public interface ProjectQueryRepository extends Repository<Project, Long> {

	Optional<Project> findById(Long projectId);

	Optional<Project> findByKeyAndWorkspace_Key(String projectKey, String workspaceKey);

	Optional<Project> findByKeyAndWorkspace(String projectKey, Workspace workspace);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Project p WHERE p.key = :key AND p.workspaceKey = :workspaceKey")
	Optional<Project> findByKeyAndWorkspaceKeyWithLock(String key, String workspaceKey);

	boolean existsByKeyAndWorkspaceKey(String projectKey, String workspaceKey);

	@Query("SELECT p.visibility FROM Project p WHERE p.key = :projectKey AND p.workspaceKey = :workspaceKey")
	Optional<ProjectVisibility> findVisibilityByKeys(
		@Param("projectKey") String projectKey,
		@Param("workspaceKey") String workspaceKey
	);
}
