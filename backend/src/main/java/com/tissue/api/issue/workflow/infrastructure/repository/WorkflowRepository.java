package com.tissue.api.issue.workflow.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.workflow.domain.model.WorkflowDefinition;

public interface WorkflowRepository extends JpaRepository<WorkflowDefinition, Long> {

	Optional<WorkflowDefinition> findByWorkspaceCodeAndKey(String workspaceCode, String key);
}
