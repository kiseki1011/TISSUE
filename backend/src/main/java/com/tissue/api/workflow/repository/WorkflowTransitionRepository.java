package com.tissue.api.workflow.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workflow.domain.model.WorkflowTransition;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {
	Optional<WorkflowTransition> findByWorkflowAndId(Workflow workflow, Long id);
}
