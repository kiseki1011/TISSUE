package com.tissue.api.issue.workflow.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.workspace.domain.model.Workspace;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

	Optional<Workflow> findByWorkspaceAndKey(Workspace workspace, String key);
}
