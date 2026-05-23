package com.tissue.feature.sprint.application.port.repository;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.SprintStatus;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(
            value = "SELECT s FROM Sprint s WHERE s.project = :project",
            countQuery = "SELECT COUNT(s) FROM Sprint s WHERE s.project = :project")
    Page<Sprint> findAllByProject(@Param("project") Project project, Pageable pageable);

    @Query(value = """
            SELECT s FROM Sprint s
            WHERE s.project = :project
              AND s.status IN :statuses
            """, countQuery = """
            SELECT COUNT(s) FROM Sprint s
            WHERE s.project = :project
              AND s.status IN :statuses
            """)
    Page<Sprint> findAllByProjectAndStatusIn(
            @Param("project") Project project, @Param("statuses") Set<SprintStatus> statuses, Pageable pageable);
}
