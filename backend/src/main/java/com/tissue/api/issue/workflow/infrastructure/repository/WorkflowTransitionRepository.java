package com.tissue.api.issue.workflow.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.issue.workflow.domain.model.WorkflowTransition;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {
	Optional<WorkflowTransition> findByWorkflowAndId(Workflow workflow, Long id);
}
