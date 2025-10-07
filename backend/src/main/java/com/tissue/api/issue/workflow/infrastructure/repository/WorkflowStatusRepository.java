package com.tissue.api.issue.workflow.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.issue.workflow.domain.model.WorkflowStatus;

public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, Long> {
	Optional<WorkflowStatus> findByWorkflowAndId(Workflow workflow, Long id);
}
