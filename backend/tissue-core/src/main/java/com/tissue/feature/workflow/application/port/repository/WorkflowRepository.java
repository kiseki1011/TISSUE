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

    @Query("SELECT w FROM Workflow w WHERE w.project = :project ORDER BY w.name.displayName ASC")
    List<Workflow> findAllByProjectOrderByLabel(@Param("project") Project project);

    @Query("""
           SELECT w
           FROM Workflow w
           JOIN FETCH w.project p
           WHERE p.workspaceKey = :workspaceKey
             AND w.id = :workflowId
       """)
    Optional<Workflow> findWithProjectByWorkspaceKeyAndId(
            @Param("workspaceKey") String workspaceKey, @Param("workflowId") Long workflowId);

    boolean existsByProjectAndName_NormalizedName(Project project, String name);

    @Query("""
           SELECT w
           FROM Workflow w
           WHERE w.workspaceKey = :workspaceKey
             AND w.id IN :ids
       """)
    List<Workflow> findAllByWorkspaceKeyAndIdIn(
            @Param("workspaceKey") String workspaceKey, @Param("ids") List<Long> ids);
}
