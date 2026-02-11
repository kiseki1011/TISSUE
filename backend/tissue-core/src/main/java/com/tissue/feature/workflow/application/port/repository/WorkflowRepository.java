package com.tissue.feature.workflow.application.port.repository;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.domain.Workflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkflowRepository extends Repository<Workflow, Long> {

    Workflow save(Workflow workflow);

    void delete(Workflow workflow);

    @Query("""
           SELECT w
           FROM Workflow w
           JOIN FETCH w.project p
           WHERE p.workspaceKey = :workspaceKey
             AND p.key = :projectKey
             AND w.id = :workflowId
       """)
    Optional<Workflow> findWithProjectByWorkspaceKeyAndProjectKeyAndId(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("workflowId") Long workflowId);

    @Query("SELECT w FROM Workflow w WHERE w.project = :project ORDER BY w.name.display ASC")
    List<Workflow> findAllByProjectOrderByLabel(@Param("project") Project project);

    boolean existsByProjectAndName_Normalized(Project project, String name);
}
