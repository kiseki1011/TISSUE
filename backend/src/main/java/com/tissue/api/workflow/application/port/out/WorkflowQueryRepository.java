package com.tissue.api.workflow.application.port.out;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.domain.Workflow;

public interface WorkflowQueryRepository extends Repository<Workflow, Long> {

	Optional<Workflow> findById(Long id);

	Optional<Workflow> findByIdAndProject(Long id, Project project);

	Optional<Workflow> findByIdAndProject_Key(Long id, String projectKey);

	@Query("SELECT w FROM Workflow w WHERE w.project = :project ORDER BY w.label.display ASC")
	List<Workflow> findAllByProjectOrderByLabel(@Param("project") Project project);

	@Query("SELECT w FROM Workflow w WHERE w.project = :project AND w.archived = false ORDER BY w.label.display ASC")
	List<Workflow> findAllByProjectAndArchivedFalseOrderByLabel(@Param("project") Project project);

	boolean existsByProjectAndLabel_Normalized(Project project, String label);
}
