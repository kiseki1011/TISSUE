package com.tissue.workflow.application.port.out;

import com.tissue.project.domain.Project;
import com.tissue.workflow.domain.Workflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkflowQueryRepository extends Repository<Workflow, Long> {

    Optional<Workflow> findById(Long id);

    Optional<Workflow> findByIdAndProject(Long id, Project project);

    Optional<Workflow> findByIdAndProject_Key(Long id, String projectKey);

    @Query("SELECT w FROM Workflow w WHERE w.project = :project ORDER BY w.name.display ASC")
    List<Workflow> findAllByProjectOrderByLabel(@Param("project") Project project);

    @Query(
            "SELECT w FROM Workflow w WHERE w.project = :project AND w.archived = false ORDER BY"
                    + " w.name.display ASC")
    List<Workflow> findAllByProjectAndArchivedFalseOrderByLabel(@Param("project") Project project);

    boolean existsByProjectAndName_Normalized(Project project, String name);
}
