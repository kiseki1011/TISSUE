package com.tissue.api.issue.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.domain.newmodel.WorkflowDefinition;

public interface WorkflowRepository extends JpaRepository<WorkflowDefinition, Long> {

	Optional<WorkflowDefinition> findByWorkspaceCodeAndKey(String workspaceCode, String key);
}
