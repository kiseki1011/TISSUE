package com.tissue.feature.project.application.port.out;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectVisibility;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectQueryRepository extends Repository<Project, Long> {

    Optional<Project> findById(Long projectId);

    Optional<Project> findByWorkspaceKeyAndKey(String workspaceKey, String projectKey);

    @Query("SELECT p FROM Project p "
            + "JOIN FETCH p.workspace "
            + "WHERE p.workspaceKey = :workspaceKey "
            + "AND p.key = :key")
    Optional<Project> findWithWorkspaceByWorkspaceKeyAndProjectKey(
            @Param("workspaceKey") String workspaceKey, @Param("key") String projectKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.key = :key AND p.workspaceKey = :workspaceKey")
    Optional<Project> findByKeyAndWorkspaceKeyWithLock(String key, String workspaceKey);

    boolean existsByKeyAndWorkspaceKey(String projectKey, String workspaceKey);

    @Query("SELECT p.visibility FROM Project p WHERE p.key = :projectKey AND p.workspaceKey =" + " :workspaceKey")
    Optional<ProjectVisibility> findVisibilityByKeys(
            @Param("workspaceKey") String workspaceKey, @Param("projectKey") String projectKey);
}
