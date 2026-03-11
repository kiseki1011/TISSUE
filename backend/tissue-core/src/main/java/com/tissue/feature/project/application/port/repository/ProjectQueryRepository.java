package com.tissue.feature.project.application.port.repository;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectVisibility;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectQueryRepository extends Repository<Project, Long> {

    Optional<Project> findByWorkspaceKeyAndKey(String workspaceKey, String projectKey);

    @Query("SELECT p FROM Project p "
            + "JOIN FETCH p.workspace "
            + "WHERE p.workspaceKey = :workspaceKey "
            + "AND p.key = :key")
    Optional<Project> findWithWorkspaceByWorkspaceKeyAndProjectKey(
            @Param("workspaceKey") String workspaceKey, @Param("key") String projectKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.workspaceKey = :workspaceKey AND p.key = :projectKey")
    Optional<Project> findByWorkspaceKeyAndProjectKeyWithLock(String workspaceKey, String projectKey);

    boolean existsByKeyAndWorkspaceKey(String projectKey, String workspaceKey);

    int countByWorkspaceKey(String workspaceKey);

    @Query("SELECT p.visibility FROM Project p WHERE p.key = :projectKey AND p.workspaceKey = :workspaceKey")
    Optional<ProjectVisibility> findVisibilityByKeys(
            @Param("workspaceKey") String workspaceKey, @Param("projectKey") String projectKey);

    @Query(value = """
            SELECT p.*
            FROM project p
            WHERE p.workspace_key = :workspaceKey
              AND p.project_key = :projectKey
              AND p.soft_deleted = true
            """, nativeQuery = true)
    Optional<Project> findDeletedByWorkspaceKeyAndKey(
            @Param("workspaceKey") String workspaceKey, @Param("projectKey") String projectKey);
}
