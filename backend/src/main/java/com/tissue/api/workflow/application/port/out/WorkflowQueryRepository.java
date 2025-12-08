package com.tissue.api.workflow.application.port.out;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.domain.Workflow;

public interface WorkflowQueryRepository extends Repository<Workflow, Long> {

	Optional<Workflow> findById(Long id);

	Optional<Workflow> findByIdAndProject(Long id, Project project);

	Optional<Workflow> findByIdAndProject_Key(Long id, String projectKey);

	Optional<Workflow> findByIdAndProject_KeyAndProject_Workspace_Key(Long id, String projectKey, String workspaceKey);

	boolean existsByProjectAndLabel_Normalized(Project project, String label);
}
