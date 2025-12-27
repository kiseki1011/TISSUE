package com.tissue.workflow.application.port.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowTransition;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

	Optional<WorkflowTransition> findByIdAndWorkflow(Long id, Workflow workflow);
}
