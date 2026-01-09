package com.tissue.project.application.port.out;

import com.tissue.project.domain.Project;
import com.tissue.project.domain.enums.ProjectVisibility;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectQueryRepository extends Repository<Project, Long> {

    Optional<Project> findById(Long projectId);

    Optional<Project> findByKeyAndWorkspaceKey(String projectKey, String workspaceKey);

    @Query("SELECT p FROM Project p JOIN FETCH p.workspace WHERE p.key = :key AND p.workspaceKey = :workspaceKey")
    Optional<Project> findWithWorkspaceByKeyAndWorkspaceKey(
            @Param("key") String projectKey, @Param("workspaceKey") String workspaceKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.key = :key AND p.workspaceKey = :workspaceKey")
    Optional<Project> findByKeyAndWorkspaceKeyWithLock(String key, String workspaceKey);

    boolean existsByKeyAndWorkspaceKey(String projectKey, String workspaceKey);

    @Query("SELECT p.visibility FROM Project p WHERE p.key = :projectKey AND p.workspaceKey =" + " :workspaceKey")
    Optional<ProjectVisibility> findVisibilityByKeys(
            @Param("projectKey") String projectKey, @Param("workspaceKey") String workspaceKey);
}
