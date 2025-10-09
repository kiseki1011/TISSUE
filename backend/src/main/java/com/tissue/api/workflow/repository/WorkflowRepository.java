package com.tissue.api.workflow.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workspace.domain.model.Workspace;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

	Optional<Workflow> findByWorkspaceAndId(Workspace workspace, Long id);

	boolean existsByWorkspaceAndLabel_Normalized(Workspace workspace, String label);
}
