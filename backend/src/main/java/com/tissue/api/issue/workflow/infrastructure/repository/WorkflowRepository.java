package com.tissue.api.issue.workflow.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.workflow.domain.model.Workflow;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

	Optional<Workflow> findByWorkspaceCodeAndKey(String workspaceCode, String key);
}
