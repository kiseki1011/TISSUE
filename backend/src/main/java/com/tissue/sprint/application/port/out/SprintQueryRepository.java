package com.tissue.sprint.application.port.out;

import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.SprintStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface SprintQueryRepository extends Repository<Sprint, Long> {

    Optional<Sprint> findByProjectAndId(Project project, Long id);

    Optional<Sprint> findByProject_KeyAndId(String projectKey, Long id);

    @Query("""
           SELECT s
           FROM Sprint s
           JOIN FETCH s.project p
           WHERE p.workspaceKey = :workspaceKey
             AND p.key = :projectKey
             AND s.id = :sprintId
       """)
    Optional<Sprint> findWithProjectByWorkspaceKeyAndProjectKeyAndId(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("sprintId") Long sprintId);

    Optional<Sprint> findByProjectAndStatus(Project project, SprintStatus status);
}
