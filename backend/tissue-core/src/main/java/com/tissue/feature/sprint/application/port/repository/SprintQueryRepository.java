package com.tissue.feature.sprint.application.port.repository;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.SprintStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface SprintQueryRepository extends Repository<Sprint, Long> {

    Optional<Sprint> findByProjectAndId(Project project, Long id);

    @Query("""
           SELECT s
           FROM Sprint s
           JOIN FETCH s.project p
           WHERE s.workspaceKey = :workspaceKey
             AND s.id = :sprintId
       """)
    Optional<Sprint> findWithProjectByWorkspaceKeyAndId(
            @Param("workspaceKey") String workspaceKey, @Param("sprintId") Long sprintId);

    Optional<Sprint> findByProjectAndStatus(Project project, SprintStatus status);
}
