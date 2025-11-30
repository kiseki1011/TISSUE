package com.tissue.api.workflow.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.domain.Workflow;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

	Optional<Workflow> findByProjectAndId(Project project, Long id);

	boolean existsByProjectAndLabel_Normalized(Project project, String label);
}
